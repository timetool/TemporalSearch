package de.l3s.common.features.hadoop;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.log4j.Logger;

import de.l3s.common.models.timeseries.KeyData;
import de.l3s.common.models.timeseries.Timeseries;

public class TimeSeriesMapper  extends MapReduceBase implements
Mapper<Text, Text, Text, Timeseries> {
	
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
	
	public static final String DATE = "date";
	private JobConf configuration;
	
	private final Timeseries timeSeries = new Timeseries();
	
	private static final Logger logger = Logger
	.getLogger(TimeSeriesMapper.class);
	
	public void configure(JobConf conf) {
		this.configuration = conf;

	}

	@Override
	public void map(Text key, Text value,
			OutputCollector<Text, Timeseries> output, Reporter reporter)
			throws IOException {
		String[] lines = value.toString().split("\n");
		
		timeSeries.ts_points = new KeyData[lines.length - 1];
		
		int idx = 0;
		for (String line : lines) {
			String[] values = line.split("\t");
			if (values[0].equals(DATE)) return;
			
			try {
				timeSeries.ts_points[idx].key.set(key.toString(), sdf.parse(values[0]).getTime());
				
				timeSeries.ts_points[idx].dataPoint.fValue = Float.parseFloat(values[1]);
				timeSeries.ts_points[idx].dataPoint.lDateTime = sdf.parse(values[0]).getTime();
				idx ++;
			} catch (ParseException e) {
				return;
			}
			
			//key to group related time series
			output.collect(key, timeSeries);
	
		}
		
		
	}

	

}
