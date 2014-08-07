package de.l3s.content.timex.extracting;

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

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.clueweb.clueweb09.ClueWeb09WarcRecord;
import org.clueweb.clueweb09.mapreduce.ClueWeb09InputFormat;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;
import de.l3s.content.timex.extracting.utils.DateUtil;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeAnnotator;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

public class ClueWeb09TimexWriteToHDFS extends Configured implements Tool{
	private static final Logger LOG = Logger.getLogger(ClueWeb09TimexWriteToHDFS.class);
	//HBase content column family
	public static final String CONTENT_CF = "content_cf";
	//HBase content quantifier
	public static final String CLEANED = "cleaned";
	//HBase content quantifier
	public static final String PUBDATE = "pubdate";
	//HBase content column family
	public static final String TEMPEX_CF = "tempex";
	//HBase tempex quantifier
	public static final String RAW = "raw";
	//HBase tempex quantifier
	public static final String ANNOTATED = "annotated";
	//date extraction confident level
	public static final String VERY_WEAK = "very weak";
	public static final String WEAK = "weak";
	public static final String STRONG = "strong";
	public static final String MILDLY_STRONG = "mildly strong";
	public static final String VERY_STRONG = "very strong";

	public enum Counters { LINES }
	//Sample Date: Mon, 14 Apr 2008 10:05:10 GMT
	static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss", Locale.ROOT);
	static SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy", Locale.ROOT);
	static SimpleDateFormat simpleDateFormat3 = new SimpleDateFormat("E, dd-MMM-yyyy HH:mm:ss", Locale.ROOT);
	private static Pattern timex3Date = Pattern.compile("<TIMEX3 tid=\"t(\\d+)\" type=\"DATE\" value=\"([^\"]+)\">([^<]?)</TIMEX3>", Pattern.MULTILINE);
	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final static IntWritable one = new IntWritable(1);
	public static String parseDate(String gmtDate) {
		gmtDate = gmtDate.replace(" GMT", "");
		gmtDate = gmtDate.replace("GMT", "");
		gmtDate = gmtDate.replaceAll("^ +| +$|( )+", "$1");
		Date parsedDate = null;
		try {
			parsedDate = simpleDateFormat.parse(gmtDate);
		} catch (ParseException e2) {
			try {
				parsedDate = simpleDateFormat2.parse(gmtDate);
			} catch (ParseException e3) {
				try {
					parsedDate = simpleDateFormat3.parse(gmtDate);
				} catch (ParseException e4) {
				}
			}
		}
		try {
			return dateFormat.format(parsedDate);
		} catch (NullPointerException npe) {
			return "2009-03-01";
		}
	}

	private static class TMapper
	extends Mapper<LongWritable, ClueWeb09WarcRecord, Text, Writable> {
		private byte[] family = null;
		private byte[] qualifier = null;

		@Override
		protected void setup(Context context)
		throws IOException, InterruptedException {
		}

		@Override
		public void map(LongWritable key, ClueWeb09WarcRecord doc, Context context)
		throws IOException, InterruptedException {


			String docid = doc.getDocid();

			String url = doc.getHeaderMetadataItem("WARC-Target-URI");
			
			LOG.info(url);
			/**
		    if (url == null) return;
			String site = new URL(url).getHost();

			context.write(new Text(site), one);
			 */
			Pair<String, String> docDate = null;
			if (doc.getHeaderMetadataItem("Last-Modified") != null) docDate = Pair.makePair(parseDate(doc.getHeaderMetadataItem("Last-Modified")), WEAK);
			else if (doc.getHeaderMetadataItem("Date") != null) docDate = Pair.makePair(parseDate(doc.getHeaderMetadataItem("Date")), VERY_WEAK);
			else docDate = Pair.makePair("2009-03-01", "N/A");
			if (docid != null) {
				try {
					//clean html
					String content = ArticleExtractor.INSTANCE.getText(doc.getContent());
					Scanner contentScanner = new Scanner(content);
					if (!contentScanner.hasNextLine()) {
						//get publication date from URL
						Pair<String, String> _docDate = DateUtil.extractDateFromURL_(url);
						docDate = (_docDate != null) ? _docDate : docDate;

						context.write(new Text(docid + "\t" + docDate.toString() + "\t" + "N/A" + "\t" + "N/A"), null);
					} else {
						String firstLines = contentScanner.nextLine() + (contentScanner.hasNextLine() ? contentScanner.nextLine() : "");
						contentScanner.close();
						//assume the publication date is from the first 2 lines
						String pubDateTags = HeidelTimeStandalone.tag(content, firstLines, DocumentType.NARRATIVES);
						Matcher date = (pubDateTags == null) ? null : timex3Date.matcher(pubDateTags);
						//the first extracted absolute date is the publication date
						if (date != null && date.find()) {
							docDate = (date.group(2).length() == "yyyy-MM-dd".length()) ? Pair.makePair(date.group(2), STRONG) : Pair.makePair(date.group(2), MILDLY_STRONG);
						} else {
							//get publication date from URL
							Pair<String, String> _docDate = DateUtil.extractDateFromURL_(url);
							docDate = (_docDate != null) ? _docDate : docDate;
						}
						String timetags;
						StringBuffer _timetags = new StringBuffer();
						//if publication date is not extracted then very likely it is not a web article
						LOG.info("Doc date:" + docDate.toString());
						if (!docDate.second.contains("strong")) {
							//content = DefaultExtractor.INSTANCE.getText(doc.getContent());
							//reference point is not important here
							//for HeidelTime
							timetags = HeidelTimeStandalone.tag(content, docDate.first, DocumentType.NARRATIVES);
							//annotation is not necessary here 
						} else {
							timetags = HeidelTimeStandalone.tag(content, docDate.first, DocumentType.COLLOQUIAL);
							ArrayList<Triple<String, String, String>>  triples = HeidelTimeAnnotator.annotate(content, docDate.first);
							for (Triple<String, String, String> triple : triples) {
								_timetags.append(triple.toString());
							}
						}

						LOG.info("Doc date: " + docDate.toString());
						LOG.info("Time tags: " + timetags);

						//context.getCounter(Counters.LINES).increment(1);

						context.write(new Text(docid + "\t" + docDate.toString() + "\t" + timetags.toString() + "\t" + _timetags.toString()), null);

					}
				} catch (BoilerpipeProcessingException e) {}
				catch (ArrayIndexOutOfBoundsException ae) {}
			}
		}
	}


	public static class IntSumReducer 
	extends Reducer<Text,IntWritable,Text,IntWritable> {
		private IntWritable result = new IntWritable();

		public void reduce(Text key, Iterable<IntWritable> values, 
				Context context
		) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			result.set(sum);
			context.write(key, result);
		}
	}

	public static final String INPUT_OPTION = "input";
	public static final String OUTPUT_OPTION = "output";
	public static final String COLUMN = "column";
	/**
	 * Runs this tool.
	 */
	@SuppressWarnings("static-access")
	public int run (String[] args) throws Exception {
		Options options = new Options();

		options.addOption(OptionBuilder.withArgName("input").hasArg()
				.withDescription("input path").create(INPUT_OPTION));

		options.addOption(OptionBuilder.withArgName("output").hasArg()
				.withDescription("output path").create(OUTPUT_OPTION));

		options.addOption(OptionBuilder.withArgName("column").hasArg()
				.withDescription("column to store row data into (must exist)").create(COLUMN));
		CommandLine cmdline;
		CommandLineParser parser = new GnuParser();
		cmdline = parser.parse(options, args);

		if (!cmdline.hasOption(INPUT_OPTION)) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(this.getClass().getName(), options);
			ToolRunner.printGenericCommandUsage(System.out);
			return -1;
		}

		if (!cmdline.hasOption(OUTPUT_OPTION)) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(this.getClass().getName(), options);
			ToolRunner.printGenericCommandUsage(System.out);
			return -1;
		}

		String input = cmdline.getOptionValue(INPUT_OPTION);

		String output = cmdline.getOptionValue(OUTPUT_OPTION);

		LOG.info("Tool name: " + ClueWeb09TimexWriteToHDFS.class.getSimpleName());
		LOG.info(" - input: " + input);
		LOG.info(" - output: " + output);
		
		
		
		Configuration conf = new Configuration();
		long milliSeconds = 10000*60*60; //x10 default
		conf.setLong("mapred.task.timeout", milliSeconds);
		Job job = Job.getInstance(conf, "extract CW tempex and output to HDFS");
		job.setJarByClass(ClueWeb09TimexWriteToHDFS.class);
		job.setNumReduceTasks(0);



		job.setInputFormatClass(ClueWeb09InputFormat.class);
		job.setMapperClass(TMapper.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));
		job.waitForCompletion(true);

		return 0;
	}

	/**
	 * Dispatches command-line arguments to the tool via the <code>ToolRunner</code>.
	 */
	public static void main(String[] args) throws Exception {
		LOG.info("Running " + ClueWeb09TimexWriteToHDFS.class.getCanonicalName() + " with args "
				+ Arrays.toString(args));
		ToolRunner.run(new ClueWeb09TimexWriteToHDFS(), args);
	}


}
