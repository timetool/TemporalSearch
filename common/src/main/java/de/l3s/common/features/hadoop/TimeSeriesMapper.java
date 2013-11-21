package de.l3s.common.features.hadoop;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.log4j.Logger;

import de.l3s.common.models.timeseries.KeyData;
import de.l3s.common.models.timeseries.Timeseries;

public class TimeSeriesMapper  extends  Mapper<LongWritable, Text, Text, Timeseries> {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

	public static final String DATE = "date";

	private final Timeseries timeSeries = new Timeseries();

	private static final Logger logger = Logger
	.getLogger(TimeSeriesMapper.class);

	public void map(LongWritable key, Text value,
			Context context)
	throws IOException, InterruptedException {
		// Get the name of the file from the inputsplit in the context
        String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();
		logger.info("fname: " + fileName);
		logger.info("content: " + value.toString());
		String[] lines = value.toString().split("\\n");
		 logger.info("number of lines:  " + lines.length);
		timeSeries.ts_points = new KeyData[lines.length - 1];
        logger.info("timeseries length: " + timeSeries.ts_points.length);
		int idx = 0;
		for (String line : lines) {
			logger.info("line: " + line );
			String[] values = line.split("\\t");
			logger.info("date: " +  values[0]);
			logger.info("freq: " +  values[1]);
			if (values[0].equals(DATE)) continue;

			try {
				logger.info("date: " +  values[0]);
				timeSeries.ts_points[idx].key.set(key.toString(), sdf.parse(values[0]).getTime());

				timeSeries.ts_points[idx].dataPoint.fValue = Float.parseFloat(values[1]);
				timeSeries.ts_points[idx].dataPoint.lDateTime = sdf.parse(values[0]).getTime();
				idx ++;
			} catch (ParseException e) {
				return;
			}
			Text mapKey = new Text();
			mapKey.set(fileName);
			//key to group related time series
			context.write(mapKey, timeSeries);

		}
	}

}
