package de.l3s.common.features.hadoop.autocorrelation;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import de.l3s.common.models.timeseries.TimeseriesDataPoint;
import de.l3s.common.models.timeseries.TimeseriesKey;



public class CorrelationMapper extends MapReduceBase implements
Mapper<LongWritable, Text, TimeseriesKey, TimeseriesDataPoint> {

	@Override
	public void map(LongWritable arg0, Text arg1,
			OutputCollector<TimeseriesKey, TimeseriesDataPoint> arg2,
			Reporter arg3) throws IOException {
		// TODO Auto-generated method stub
		
	}
	



}
