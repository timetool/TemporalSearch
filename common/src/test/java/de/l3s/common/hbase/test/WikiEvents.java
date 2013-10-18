package de.l3s.common.hbase.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import au.com.bytecode.opencsv.CSVReader;

public class WikiEvents {
	Configuration conf;
	HBaseAdmin admin;
	
	public void init() throws MasterNotRunningException, ZooKeeperConnectionException {
		conf = HBaseConfiguration.create();
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
		CSVReader reader = new CSVReader(new InputStreamReader(WikiEvents.class.getResourceAsStream(path)),'\t');
		String[] entry;
		ArrayList<String[]> entries = new ArrayList<String[]>();
		while ((entry = reader.readNext()) != null) {
			entries.add(entry);
		}		
	}
	
	public void putTimeSeriesData(HTable table, String csvPath) {
		//get event name frome file path
		File eventFile = new File (csvPath);
		String eventName = eventFile.getName().replace(".vtime", "");
		CSVReader reader = new CSVReader(new InputStreamReader(WikiEvents.class.getResourceAsStream(csvPath)),'\t');
		String[] entry;
		try {
			ArrayList<String[]> entries = new ArrayList<String[]>();
			while ((entry = reader.readNext()) != null) {
				entries.add(entry);
			}	
            //eventName is a row key
			Put put = new Put(Bytes.toBytes(eventName));
			put.add(Bytes.toBytes("cf"), Bytes.toBytes("q"), Bytes.toBytes("value"));
			table.put(put);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}
	}

}
