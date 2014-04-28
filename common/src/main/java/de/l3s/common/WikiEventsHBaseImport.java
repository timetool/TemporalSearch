package de.l3s.common;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVReader;

public class WikiEventsHBaseImport {
	Configuration conf;
	HBaseAdmin admin;

	public void init() throws MasterNotRunningException, ZooKeeperConnectionException {
		conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", "master.hadoop,node01.hadoop,node02.hadoop");
		conf.set("hbase.zookeeper.property.clientPort","2181");
		conf.set("hbase.master", "master.hadoop");
		admin = new HBaseAdmin(conf);	
	}

	/**
	 * 
	 * @param tableName e.g., WikiEvent
	 * @return
	 * @throws IOException
	 */
	public HTable createTable(String tableName) throws IOException {
		HTable hTable = new HTable(conf, tableName);
		return hTable;
	}

	public void readCSV(String path) throws IOException {
		CSVReader reader = new CSVReader(new InputStreamReader(WikiEventsHBaseImport.class.getResourceAsStream(path)),'\t');
		String[] entry;
		ArrayList<String[]> entries = new ArrayList<String[]>();
		while ((entry = reader.readNext()) != null) {
			entries.add(entry);
		}		
	}

	/**
	 * schema:
	 * "event:"{
	 *    "lang" : {
	 *               "date1":~
	 *               "date2":~
	 *             }
	 *         }
	 * @param table
	 * @param csvPath
	 * @throws FileNotFoundException 
	 */
	public void putTimeSeriesDataToHBase(HTable table, File csvFile) throws FileNotFoundException {
		//get event name frome file path
		String eventName = csvFile.getName().replace(".vtime", "").replace(" ","_");
		System.out.println(csvFile.getAbsolutePath());
		//transpose csv file
		HashMap<String, String[]> dataMap = new HashMap<String, String[]>();
		CSVReader reader = new CSVReader(new FileReader(csvFile),'\t');
		String[] titles = null;
		String[] entry;
		try {
			//csv titles
			titles = reader.readNext();
			while ((entry = reader.readNext()) != null) {
				//key: date, value: entry
				dataMap.put(entry[0], entry);	
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Set<String> dates = dataMap.keySet();

		//eventName is a row key
		Put put = new Put(Bytes.toBytes(eventName));

		for (String date : dates) {
			for (int idx = 1; idx < titles.length; idx++) {
				//column family:lang, column quantifier:date
				put.add(Bytes.toBytes(titles[idx]), Bytes.toBytes(date), Bytes.toBytes(dataMap.get(date)[idx]));
			}
		}
		try {
			table.put(put);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		WikiEventsHBaseImport we = new WikiEventsHBaseImport();
		HTable htable = null;
		try {
			we.init();
			htable = we.createTable("WikiEvents");
		} catch (MasterNotRunningException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ZooKeeperConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File inputDir = new File(args[0]);

		Set<String> filenameSet = new HashSet<String>();
		Set<File> fileSet_ = new HashSet<File>();

		//iterative read all files in the directory tree
		Set<File> fileSet = we.listFileTree(inputDir);
		for (File f : fileSet) {
			if (!f.isDirectory() && !filenameSet.contains(f.getName())) {
				filenameSet.add(f.getName());
				fileSet_.add(f);
			}
		}

		//dump data
		for (File f : fileSet_) {
			try {
				we.putTimeSeriesDataToHBase(htable, f);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			we.admin.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @param dir
	 * @return
	 */
	public Set<File> listFileTree(File dir) {
		Set<File> fileTree = new HashSet<File>();
		for (File entry : dir.listFiles(filter)) {
			if (entry.isFile()) fileTree.add(entry);
			else fileTree.addAll(listFileTree(entry));
		}
		return fileTree;
	}

	final FileFilter filter = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().endsWith(".vtime");
		}
	};

	/**
	 * Sometimes, you won't know the row you're looking for. In this case, you
	 * use a Scanner. This will give you cursor-like interface to the contents
	 * of the table.  To set up a Scanner, do like you did above making a Put
	 * and a Get, create a Scan.  Adorn it with column names, etc.
	 */
	public List<Result> scanHBase(Scan s, String family, String qualifier, HTable table) {
		List<Result> l = Lists.newArrayList();
		s.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
		try {
			ResultScanner scanner = table.getScanner(s);
			try {
				for (Result rr = scanner.next(); rr != null; rr = scanner.next()) {
					l.add(rr);
				} 
			} finally {
				scanner.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		return l;
	}


}
