package de.l3s.common.features.hadoop;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import de.l3s.common.models.timeseries.Timeseries;

public class TimeSeriesJob  extends Configured implements Tool{
	private static final String JOB_NAME = "name";
	private static final String INPUT_OPT = "in";
	private static final String OUTPUT_OPT = "out";
	private static final String REDUCE_NO = "reduce";
	private static final String REMOVE_OUTPUT = "rmo";
	private static final String COMPRESS_OPT = "compress";

	private static final int DEFAULT_REDUCER_NO = 24;

	@Override
	public int run(String[] args) throws Exception {
		Options opts = new Options();

		Option jnameOpt = OptionBuilder.withArgName("job-name").hasArg(true)
				.withDescription("Timeseries analysis")
				.create(JOB_NAME);

		Option inputOpt = OptionBuilder.withArgName("input-path").hasArg(true)
				.withDescription("Timeseries file path (required)")
				.create(INPUT_OPT);

		Option outputOpt = OptionBuilder.withArgName("output-path").hasArg(true)
				.withDescription("output file path (required)")
				.create(OUTPUT_OPT);

		Option reduceOpt = OptionBuilder.withArgName("reduce-no").hasArg(true)
				.withDescription("number of reducer nodes").create(REDUCE_NO);

		Option rmOpt = OptionBuilder.withArgName("remove-out").hasArg(false)
				.withDescription("remove the output then create again before writing files onto it")
				.create(REMOVE_OUTPUT);

		Option cOpt = OptionBuilder.withArgName("compress-option").hasArg(true)
				.withDescription("compression option").create(COMPRESS_OPT);

		opts.addOption(jnameOpt);
		opts.addOption(inputOpt);
		opts.addOption(reduceOpt);
		opts.addOption(outputOpt);
		opts.addOption(rmOpt);
		opts.addOption(cOpt);
		CommandLine cl;
		CommandLineParser parser = new GnuParser();
		try {
			cl = parser.parse(opts, args);
		} catch (ParseException e) {
			System.err.println("Error parsing command line: " + e.getMessage());
			return -1;
		}

		if (!cl.hasOption(INPUT_OPT) || !cl.hasOption(OUTPUT_OPT)) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(getClass().getName(), opts);
			ToolRunner.printGenericCommandUsage(System.out);
			return -1;
		}

		int reduceNo = DEFAULT_REDUCER_NO;
		if (cl.hasOption(REDUCE_NO)) {
			try {
				reduceNo = Integer.parseInt(cl.getOptionValue(REDUCE_NO));
			} catch (NumberFormatException e) {
				System.err.println("Error parsing reducer number: "
						+ e.getMessage());
			}
		}

		String jobName = "Timeseries correlation";
		if (cl.hasOption(JOB_NAME)) {
			jobName = cl.getOptionValue(JOB_NAME);
			jobName = jobName.replace('-', ' ');
		}

		if (cl.hasOption(REMOVE_OUTPUT)) {

		}


		String input = cl.getOptionValue(INPUT_OPT);
		
		///user/nguyen/WikiTS/output
		String output = cl.getOptionValue(OUTPUT_OPT);


		JobConf conf = new JobConf(getConf(), TimeSeriesJob.class);
		conf.setJobName(jobName);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Timeseries.class);

		conf.setNumReduceTasks(reduceNo);

		conf.setInputFormat(TextInputFormat.class);

		conf.setOutputFormat(TextOutputFormat.class);
		conf.setCompressMapOutput(true);

		FileInputFormat.setInputPaths(conf, input);
		FileOutputFormat.setOutputPath(conf, new Path(output));

		JobClient.runJob(conf);

		return 0;
	}


	public static void main(String[] args) throws Exception {

		int res = ToolRunner.run(new Configuration(), new TimeSeriesJob(),
				args);
		System.exit(res);

	}

}
