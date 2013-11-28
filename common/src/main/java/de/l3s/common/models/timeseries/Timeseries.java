package de.l3s.common.models.timeseries;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Writable;

public class Timeseries implements Writable{
	
	public ArrayList<KeyData> ts_points;
	
	public Timeseries(int length) {
		ts_points = new ArrayList<KeyData>(length);
		for(int i = 0; i < length; i++) {
			ts_points.add(new KeyData());
		}
	}
	
	@Override
	public void write(DataOutput out) {
		for (KeyData keyData : ts_points) {
			try {
				keyData.dataPoint.write(out);
				keyData.key.write(out);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}


	@Override
	public void readFields(DataInput in) throws IOException {
		for (KeyData keyData : ts_points) {
			try {
				keyData.dataPoint.readFields(in);
				keyData.key.readFields(in);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
