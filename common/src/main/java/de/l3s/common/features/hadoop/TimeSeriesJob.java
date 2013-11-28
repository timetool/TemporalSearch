package de.l3s.common.features.hadoop;

/*
 * TIMETool - Large-scale Temporal Search in MapReduce
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * THIS SOFTWARE IS PROVIDED BY THE LEMUR PROJECT AS PART OF THE CLUEWEB09
 * PROJECT AND OTHER CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @author 
 */
import java.net.URI;

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
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import de.l3s.common.hadoop.WholeFileInputFormat;
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

		String jobName = "Distributed timeseries [R] correlation";
		if (cl.hasOption(JOB_NAME)) {
			jobName = cl.getOptionValue(JOB_NAME);
			jobName = jobName.replace('-', ' ');
		}

		if (cl.hasOption(REMOVE_OUTPUT)) {

		}

		String input = cl.getOptionValue(INPUT_OPT);
		String output = cl.getOptionValue(OUTPUT_OPT);

		Configuration conf = getConf();
		DistributedCache.createSymlink(conf); 
		DistributedCache.addCacheFile(new URI("hdfs://master.hadoop:8020/user/nguyen/lib/"), conf);
		Job job = new Job(conf, jobName);
        job.setJarByClass(TimeSeriesJob.class);
		job.setMapperClass(TimeSeriesMapper.class);
		job.setReducerClass(TimeSeriesReducer.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Timeseries.class);
		
        
		job.setNumReduceTasks(reduceNo);
        job.setInputFormatClass(WholeFileInputFormat.class);
		WholeFileInputFormat.setInputPaths(job, input);
		FileOutputFormat.setOutputPath(job, new Path(output));

		return job.waitForCompletion(true) ? 0 : 1;
	}


	public static void main(String[] args) throws Exception {

		int res = ToolRunner.run(new Configuration(), new TimeSeriesJob(),
				args);
		System.exit(res);

	}

}
