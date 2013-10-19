package de.l3s.common.features.hadoop.autocorrelation;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import de.l3s.common.models.timeseries.TimeseriesDataPoint;
import de.l3s.common.models.timeseries.TimeseriesKey;


public class CorrelationReducer extends MapReduceBase implements
Reducer<TimeseriesKey, TimeseriesDataPoint, Text, Text> {

	@Override
	public void reduce(TimeseriesKey arg0, Iterator<TimeseriesDataPoint> arg1,
			OutputCollector<Text, Text> arg2, Reporter arg3) throws IOException {
		// TODO Auto-generated method stub
		
	}

}

