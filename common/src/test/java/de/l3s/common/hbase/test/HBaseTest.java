package de.l3s.common.hbase.test;

/*
 * Compile and run with:
 * javac -cp `hbase classpath` TestHBase.java 
 * java -cp `hbase classpath` TestHBase
 */
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.*;

public class HBaseTest {
	public static void main(String[] args) throws Exception {
		Configuration conf = HBaseConfiguration.create();
		HBaseAdmin admin = new HBaseAdmin(conf);
		try {
			HTable table = new HTable(conf, "test-table");
			Put put = new Put(Bytes.toBytes("test-key"));
			put.add(Bytes.toBytes("cf"), Bytes.toBytes("q"), Bytes.toBytes("value"));
			table.put(put);
		} finally {
		}
	}
}

