package de.l3s.common.features.hadoop;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import com.google.common.collect.Lists;

import de.l3s.common.models.timeseries.KeyData;
import de.l3s.common.models.timeseries.Timeseries;


public class TimeSeriesReducer extends MapReduceBase implements
Reducer<Text, Timeseries, Text, DoubleWritable> {
	
	private JobConf configuration;

	
	TimeSeriesFeatures eval = new TimeSeriesFeatures();
	
	@Override
	public void configure(JobConf job) {

		this.configuration = job;

	}

	@Override
	public void reduce(Text key, Iterator<Timeseries> values,
			OutputCollector<Text, DoubleWritable> output, Reporter reporter) throws IOException {
		Timeseries timeSeries;
		List<Integer> ts_list = Lists.newArrayList();
		while(values.hasNext()) {
			
			timeSeries = values.next();
			
			for (KeyData keydata : timeSeries.ts_points) {
				ts_list.add((int)keydata.dataPoint.fValue);
			}
			
			double[] acf_score = eval.computeAutoCorrel(ts_list.size(), ArrayUtils.toPrimitive(ts_list.toArray(new Integer[ts_list.size()])));
			
			output.collect(key, new DoubleWritable(acf_score[0]));			
		}
		
	}

}
