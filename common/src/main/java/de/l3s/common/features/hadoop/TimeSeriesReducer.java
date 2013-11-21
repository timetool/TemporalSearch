package de.l3s.common.features.hadoop;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.google.common.collect.Lists;

import de.l3s.common.features.TimeSeriesFeatures;
import de.l3s.common.models.timeseries.KeyData;
import de.l3s.common.models.timeseries.Timeseries;


public class TimeSeriesReducer extends Reducer<Text, Timeseries, Text, DoubleWritable> {
	
	TimeSeriesFeatures eval = new TimeSeriesFeatures();

	public void reduce(Text key, Iterator<Timeseries> values,
			Context context) throws IOException, InterruptedException {
		////-Djava.library.path = /usr/lib64/R/library/rJava/jri
		Timeseries timeSeries;
		List<Integer> ts_list = Lists.newArrayList();
		while(values.hasNext()) {
			
			timeSeries = values.next();
			
			for (KeyData keydata : timeSeries.ts_points) {
				ts_list.add((int) keydata.dataPoint.fValue);
			}
			
			double[] acf_score = eval.computeAutoCorrel(ts_list.size(), ArrayUtils.toPrimitive(ts_list.toArray(new Integer[ts_list.size()])));
			
			context.write(key, new DoubleWritable(acf_score[0]));			
		}
		
	}

}
