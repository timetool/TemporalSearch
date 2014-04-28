/**
 * Copyright 2011 Yusuke Matsubara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wikimedia.wikihadoop;

import java.io.*;
import java.util.*;

import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.io.compress.*;
import org.apache.log4j.Logger;


import java.util.regex.*;

/** A InputFormat implementation that splits a Wikimedia Dump File into page fragments, and emits them as input records.
 * The record reader embedded in this input format converts a page into a sequence of page-like elements, each of which contains two consecutive revisions.  Output is given as keys with empty values.
 *
 * For example,  Given the following input containing two pages and four revisions,
 * <pre><code>
 *  &lt;page&gt;
 *    &lt;title&gt;ABC&lt;/title&gt;
 *    &lt;id&gt;123&lt;/id&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;100&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;200&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;300&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *  &lt;/page&gt;
 *  &lt;page&gt;
 *    &lt;title&gt;DEF&lt;/title&gt;
 *    &lt;id&gt;456&lt;/id&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;400&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *  &lt;/page&gt;
 * </code></pre>
 * it will produce four keys like this:
 * <pre><code>
 *  &lt;page&gt;
 *    &lt;title&gt;ABC&lt;/title&gt;
 *    &lt;id&gt;123&lt;/id&gt;
 *    &lt;revision&gt;&lt;revision beginningofpage="true"&gt;&lt;text xml:space="preserve"&gt;&lt;/text&gt;&lt;/revision&gt;&lt;revision&gt;
 *      &lt;id&gt;100&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *  &lt;/page&gt;
 * </code></pre>
 * <pre><code>
 *  &lt;page&gt;
 *    &lt;title&gt;ABC&lt;/title&gt;
 *    &lt;id&gt;123&lt;/id&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;100&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;200&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *  &lt;/page&gt;
 * </code></pre>
 * <pre><code>
 *  &lt;page&gt;
 *    &lt;title&gt;ABC&lt;/title&gt;
 *    &lt;id&gt;123&lt;/id&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;200&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *    &lt;revision&gt;
 *      &lt;id&gt;300&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *  &lt;/page&gt;
 * </code></pre>
 * <pre><code>
 *  &lt;page&gt;
 *    &lt;title&gt;DEF&lt;/title&gt;
 *    &lt;id&gt;456&lt;/id&gt;
 *    &lt;revision&gt;&lt;revision beginningofpage="true"&gt;&lt;text xml:space="preserve"&gt;&lt;/text&gt;&lt;/revision&gt;&lt;revision&gt;
 *      &lt;id&gt;400&lt;/id&gt;
 *      ....
 *    &lt;/revision&gt;
 *  &lt;/page&gt;
 * </code></pre>
 */
public class StreamWikiDumpInputFormat extends FileInputFormat<Text,Text> {

	private static final String KEY_EXCLUDE_PAGE_PATTERN = "org.wikimedia.wikihadoop.excludePagesWith";
	private static final String KEY_PREVIOUS_REVISION    = "org.wikimedia.wikihadoop.previousRevision";
	private static final String KEY_SKIP_FACTOR          = "org.wikimedia.wikihadoop.skipFactor";
	private CompressionCodecFactory compressionCodecs = null;
	private static final Logger LOG = Logger.getLogger(StreamWikiDumpInputFormat.class);
	
	public void configure(Configuration conf) {
		this.compressionCodecs = new CompressionCodecFactory(conf);
	}

	protected boolean isSplitable(FileSystem fs, Path file) {
		final CompressionCodec codec = compressionCodecs.getCodec(file);
		if (null == codec) {
			return true;
		}
		LOG.info("codec: " + codec.toString());
		return codec instanceof SplittableCompressionCodec;
	}

	/** 
	 * Generate the list of files and make them into FileSplits.
	 * @param job the job context
	 * @throws IOException
	 */
	@Override
	public List<InputSplit> getSplits(JobContext jc) throws IOException {
		List<InputSplit> splits = super.getSplits(jc);
		List<FileStatus> files = listStatus(jc);
		// Save the number of input files for metrics/loadgen
		//construct compression codecs
        configure(jc.getConfiguration());
		long totalSize = 0;                           // compute total size
		for (FileStatus file: files) {                // check we have valid files
			if (file.isDirectory()) {
				throw new IOException("Not a file: "+ file.getPath());
			}
			totalSize += file.getLen();
		}
		long minSize = Math.max(getFormatMinSplitSize(), getMinSplitSize(jc));
		//TODO: compatible 
		//int numSplits = 1;
		//long goalSize = totalSize / (numSplits == 0 ? 1 : numSplits);
		long goalSize = totalSize / 317;
		for (FileStatus file: files) {
			if (file.isDirectory()) {
				throw new IOException("Not a file: "+ file.getPath());
			}
			long blockSize = file.getBlockSize();
			long splitSize = computeSplitSize(goalSize, minSize, blockSize);
			LOG.info(String.format("goalsize=%d splitsize=%d blocksize=%d", goalSize, splitSize, blockSize));
			//System.err.println(String.format("goalsize=%d splitsize=%d blocksize=%d", goalSize, splitSize, blockSize));
			for (InputSplit x: getSplits(jc.getConfiguration(), file, pageBeginPattern, splitSize) ) 
				splits.add(x);
		}
		System.err.println("splits="+splits);
		return splits;
	}


	private FileSplit makeSplit(Path path, long start, long size, NetworkTopology clusterMap, BlockLocation[] blkLocations) throws IOException {
		//TODO: getSplitHosts(blkLocations, start, size, clusterMap)
		String[] hosts = blkLocations[blkLocations.length-1].getHosts();
//		return makeSplit(path, start, size,
//				hosts);
		return new FileSplit(path, start, size,	hosts);
	}


	public List<InputSplit> getSplits(Configuration conf, FileStatus file, String pattern, long splitSize) throws IOException {
		NetworkTopology clusterMap = new NetworkTopology();
		List<InputSplit> splits = new ArrayList<InputSplit>();
		Path path = file.getPath();
		long length = file.getLen();
		FileSystem fs = file.getPath().getFileSystem(conf);
		BlockLocation[] blkLocations = fs.getFileBlockLocations(file, 0, length);
		LOG.info("path:" + path);
		if ((length != 0) && isSplitable(fs, path)) { 

			long bytesRemaining = length;
			SeekableInputStream in = SeekableInputStream.getInstance
			(path, 0, length, fs, this.compressionCodecs);
			SplitCompressionInputStream is = in.getSplitCompressionInputStream();
			long start = 0;
			long skip = 0;
			if ( is != null ) {
				start = is.getAdjustedStart();
				length = is.getAdjustedEnd();
				is.close();
				in = null;
			}
			LOG.info("locations=" + Arrays.asList(blkLocations));
			FileSplit split = null;
			Set<Long> processedPageEnds = new HashSet<Long>();
			float factor = conf.getFloat(KEY_SKIP_FACTOR, 1.2F);

			READLOOP:
				while (((double) bytesRemaining)/splitSize > factor  &&  bytesRemaining > 0) {
					// prepare matcher
					ByteMatcher matcher;
					{
						long st = Math.min(start + skip + splitSize, length - 1);
						split = makeSplit(path,
								st,
								Math.min(splitSize, length - st),
								clusterMap, blkLocations);
						System.err.println("split move to: " + split);
						if ( in != null )
							in.close();
						if ( split.getLength() <= 1 ) {
							break;
						}
						in = SeekableInputStream.getInstance(split,
								fs, this.compressionCodecs);
						SplitCompressionInputStream cin = in.getSplitCompressionInputStream();
					}
					matcher = new ByteMatcher(in);

					// read until the next page end in the look-ahead split
					boolean reach = false;
					while ( !matcher.readUntilMatch(pageEndPattern, null, split.getStart() + split.getLength()) ) {
						if (matcher.getPos() >= length  ||  split.getLength() == length - split.getStart())
							break READLOOP;
						reach = false;
						split = makeSplit(path,
								split.getStart(),
								Math.min(split.getLength() + splitSize, length - split.getStart()),
								clusterMap, blkLocations);
						System.err.println("split extend to: " + split);
					}
					System.err.println(path + ": #" + splits.size() + " " + pageEndPattern + " found: pos=" + matcher.getPos() + " last=" + matcher.getLastUnmatchPos() + " read=" + matcher.getReadBytes() + " current=" + start + " remaining=" + bytesRemaining + " split=" + split);
					if ( matcher.getLastUnmatchPos() > 0
							&&  matcher.getPos() > matcher.getLastUnmatchPos()
							&&  !processedPageEnds.contains(matcher.getPos()) ) {
						splits.add(makeSplit(path, start, matcher.getPos() - start, clusterMap, blkLocations));
						processedPageEnds.add(matcher.getPos());
						long newstart = Math.max(matcher.getLastUnmatchPos(), start);
						bytesRemaining = length - newstart;
						start = newstart;
						skip = 0;
					} else {
						skip = matcher.getPos() - start;
					}
				}

			if (bytesRemaining > 0 && !processedPageEnds.contains(length)) {
				System.err.println(pageEndPattern + " remaining: pos=" + (length-bytesRemaining) + " end=" + length);
				splits.add(new FileSplit(path, length-bytesRemaining, bytesRemaining, 
						blkLocations[blkLocations.length-1].getHosts()));
			}
			if ( in != null )
				in.close();
		} else if (length != 0) {
			splits.add(makeSplit(path, 0, length, clusterMap, blkLocations));
		} else { 
			//Create empty hosts array for zero length files
			splits.add(makeSplit(path, 0, length, new String[0]));
		}
		return splits;
	}


	private class WikiRecordReader extends RecordReader<Text,Text> {
		private int currentPageNum;
		private Pattern exclude;
		private boolean recordPrevRevision;
		private long start;
		private long end;
		private List<Long> pageBytes;
		private SeekableInputStream  istream;
		private String revisionBeginPattern;
		private String revisionEndPattern;
		private DataOutputBuffer pageHeader;
		private DataOutputBuffer revHeader;
		private DataOutputBuffer prevRevision;
		private DataOutputBuffer pageFooter;
		private DataOutputBuffer firstDummyRevision;
		private DataOutputBuffer bufInRev;
		private DataOutputBuffer bufBeforeRev;
		private FileSystem fs;
		private FileSplit split;
		private ByteMatcher matcher;
		private Text key = new Text();
		private Text value = new Text();
		private TaskAttemptContext taskAttemptContext;

		@Override
		public void close() throws IOException {
			this.istream.close();

		}

		@Override
		public Text getCurrentKey() throws IOException, InterruptedException {
			return key;
		}

		@Override
		public Text getCurrentValue() throws IOException, InterruptedException {
			return value;
		}

		@Override
		public float getProgress() throws IOException, InterruptedException {
			float rate = 0.0f;
			if (this.end == this.start) {
				rate = 1.0f;
			} else {
				rate = ((float)(matcher.getPos() - this.start)) / ((float)(this.end - this.start));
			}
			return rate;
		}

		@Override
		public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext)
		throws IOException, InterruptedException {
			this.taskAttemptContext = taskAttemptContext;
			this.revisionBeginPattern = "<revision";
			this.revisionEndPattern   = "</revision>";
			this.pageHeader   = new DataOutputBuffer();
			this.prevRevision = new DataOutputBuffer();
			this.pageFooter = getBuffer("\n</page>\n".getBytes("UTF-8"));
			this.revHeader  = getBuffer(this.revisionBeginPattern.getBytes("UTF-8"));
			this.firstDummyRevision = getBuffer(" beginningofpage=\"true\"><text xml:space=\"preserve\"></text></revision>\n".getBytes("UTF-8"));
			this.bufInRev = new DataOutputBuffer();
			this.bufBeforeRev = new DataOutputBuffer();
			String patt = taskAttemptContext.getConfiguration().get(KEY_EXCLUDE_PAGE_PATTERN);
			this.exclude = patt != null && !"".equals(patt) ? Pattern.compile(patt): null;
			this.recordPrevRevision = taskAttemptContext.getConfiguration().getBoolean(KEY_PREVIOUS_REVISION, true); //set it true ?
			this.split = (FileSplit) inputSplit;
			// Open the file and seek to the start of the split
			this.fs = split.getPath().getFileSystem(taskAttemptContext.getConfiguration());
			configure(taskAttemptContext.getConfiguration());
			SeekableInputStream in = SeekableInputStream.getInstance(split, fs, compressionCodecs);
			SplitCompressionInputStream sin = in.getSplitCompressionInputStream();
			if ( sin == null ) {
				this.start = split.getStart();
				this.end   = split.getStart() + split.getLength();
			} else {
				this.start = sin.getAdjustedStart();
				this.end   = sin.getAdjustedEnd() + 1;
			}

			allWrite(this.prevRevision, this.firstDummyRevision);
			this.currentPageNum = -1;
			this.pageBytes = getPageBytes(this.split, this.fs, compressionCodecs, taskAttemptContext);

			this.istream = SeekableInputStream.getInstance(this.split, this.fs, compressionCodecs);
			this.matcher = new ByteMatcher(this.istream, this.istream);
			this.seekNextRecordBoundary();
			taskAttemptContext.getCounter(WikiDumpCounters.WRITTEN_REVISIONS).increment(0);
			taskAttemptContext.getCounter(WikiDumpCounters.WRITTEN_PAGES).increment(0);

		}

		@Override
		public boolean nextKeyValue() throws IOException, InterruptedException {
			//LOG.info("StreamWikiDumpInputFormat: split=" + split + " start=" + this.start + " end=" + this.end + " pos=" + this.getPos());

			while (true) {
				if ( this.nextPageBegin() < 0 ) {
					return false;
				}

				//System.err.println("0.2 check pos="+this.getPos() + " end="+this.end);//!
				if (this.currentPageNum >= this.pageBytes.size() / 2  ||  this.getReadBytes() >= this.tailPageEnd()) {
					return false;
				}

				//System.err.println("2 move to rev from: " + this.getReadBytes());//!
				if (!readUntilMatch(this.revisionBeginPattern, this.bufBeforeRev)  ||  this.getReadBytes() >= this.tailPageEnd()) { // move to the beginning of the next revision
					return false;
				}
				//System.err.println("2.1 move to rev to: " + this.getReadBytes());//!

				//System.err.println("4.5 check if exceed: " + this.getReadBytes() + " " + nextPageBegin() + " " + prevPageEnd());//!
				if ( this.getReadBytes() >= this.nextPageBegin() ) {
					// int off = (int)(this.nextPageBegin() - this.prevPageEnd());
					int off = findIndex(pageBeginPattern.getBytes("UTF-8"), this.bufBeforeRev);
					if ( off >= 0 ) {
						offsetWrite(this.pageHeader, off, this.bufBeforeRev);
						allWrite(this.prevRevision, this.firstDummyRevision);
						this.currentPageNum++;
						if ( this.exclude != null && this.exclude.matcher(new String(this.pageHeader.getData(), "UTF-8")).find() ) {
							taskAttemptContext.getCounter(WikiDumpCounters.SKIPPED_PAGES).increment(1);
							this.seekNextRecordBoundary();
						} else {
							taskAttemptContext.getCounter(WikiDumpCounters.WRITTEN_PAGES).increment(1);
							break;
						}
						//System.err.println("4.6 exceed");//!
					} else {
						throw new IllegalArgumentException();
					}
				} else {
					break;
				}
			}

			//System.err.println("4 read rev from: " + this.getReadBytes());//!
			if (!readUntilMatch(this.revisionEndPattern, this.bufInRev)) { // store the revision
				//System.err.println("no revision end" + this.getReadBytes() + " " + this.end);//!
				LOG.info("no revision end");
				return false;
			}
			//System.err.println("4.1 read rev to: " + this.getReadBytes());//!

			//System.err.println("5 read rev pos " + this.getReadBytes());//!
			byte[] record = this.recordPrevRevision ?
					writeInSequence(new DataOutputBuffer[]{ this.pageHeader,
							this.prevRevision,
							this.revHeader,
							this.bufInRev,
							this.pageFooter}):
								writeInSequence(new DataOutputBuffer[]{ this.pageHeader,
										this.bufInRev,
										this.pageFooter});
					key.set(record);
					//System.out.print(key.toString());//!
					value.set("");
					taskAttemptContext.setStatus("StreamWikiDumpInputFormat: write new record pos=" + matcher.getPos() + " bytes=" + this.getReadBytes() + " next=" + this.nextPageBegin() + " prev=" + this.prevPageEnd());
					taskAttemptContext.getCounter(WikiDumpCounters.WRITTEN_REVISIONS).increment(1);

					if ( this.recordPrevRevision ) {
						allWrite(this.prevRevision, this.bufInRev);
					}

					return true;
		}

		public synchronized void seekNextRecordBoundary() throws IOException {
			if ( this.getReadBytes() < this.nextPageBegin() ) {
				long len = this.nextPageBegin() - this.getReadBytes();
				this.matcher.skip(len);
			}
		}
		private synchronized boolean readUntilMatch(String textPat, DataOutputBuffer outBufOrNull) throws IOException {
			if ( outBufOrNull != null )
				outBufOrNull.reset();
			return this.matcher.readUntilMatch(textPat, outBufOrNull, this.end);
		}
		private long tailPageEnd() {
			if ( this.pageBytes.size() > 0 ) {
				return this.pageBytes.get(this.pageBytes.size() - 1);
			} else {
				return 0;
			}
		}
		private long nextPageBegin() {
			if ( (this.currentPageNum + 1) * 2 < this.pageBytes.size() ) {
				return this.pageBytes.get((this.currentPageNum + 1) * 2);
			} else {
				return -1;
			}
		}
		private long prevPageEnd() {
			if ( this.currentPageNum == 0 ) {
				if ( this.pageBytes.size() > 0 ) {
					return this.pageBytes.get(0);
				} else {
					return 0;
				}
			} else if ( this.currentPageNum * 2 - 1 <= this.pageBytes.size() - 1 ) {
				return this.pageBytes.get(this.currentPageNum * 2 - 1);
			} else {
				return this.pageBytes.get(this.pageBytes.size() - 1);
			}
		}  

		public synchronized long getReadBytes() throws IOException {
			return this.matcher.getReadBytes();
		}


	}
	
	private static byte[] writeInSequence(DataOutputBuffer[] array) {
		int size = 0;
		for (DataOutputBuffer buf: array) {
			size += buf.getLength();
		}
		byte[] dest = new byte[size];
		int n = 0;
		for (DataOutputBuffer buf: array) {
			System.arraycopy(buf.getData(), 0, dest, n, buf.getLength());
			n += buf.getLength();
		}
		return dest;
	}

	private static DataOutputBuffer getBuffer(byte[] bytes) throws IOException {
		DataOutputBuffer ret = new DataOutputBuffer(bytes.length);
		ret.write(bytes);
		return ret;
	}

	private static List<Long> getPageBytes(FileSplit split, FileSystem fs, CompressionCodecFactory compressionCodecs, TaskAttemptContext taskAttemptContext) throws IOException {
		SeekableInputStream in = null;
		try {
			in = SeekableInputStream.getInstance(split, fs, compressionCodecs);
			long start = split.getStart();
			long end   = start + split.getLength();
			SplitCompressionInputStream cin = in.getSplitCompressionInputStream();
			if ( cin != null ) {
				start = cin.getAdjustedStart();
				end   = cin.getAdjustedEnd() + 1;
			}
			ByteMatcher matcher = new ByteMatcher(in, in);
			List<Long> ret = new ArrayList<Long>();
			while ( true ) {
				if ( matcher.getPos() >= end || !matcher.readUntilMatch(pageBeginPattern, null, end) ) {
					break;
				}
				ret.add(matcher.getReadBytes() - pageBeginPattern.getBytes("UTF-8").length);
				if ( matcher.getPos() >= end || !matcher.readUntilMatch(pageEndPattern, null, end) ) {
					System.err.println("could not find "+pageEndPattern+", page over a split?  pos=" + matcher.getPos() + " bytes=" + matcher.getReadBytes());
					//ret.add(end);
					break;
				}
				ret.add(matcher.getReadBytes() - pageEndPattern.getBytes("UTF-8").length);
				String report = String.format("StreamWikiDumpInputFormat: find page %6d start=%d pos=%d end=%d bytes=%d", ret.size(), start, matcher.getPos(), end, matcher.getReadBytes());
				taskAttemptContext.setStatus(report);
				taskAttemptContext.getCounter(WikiDumpCounters.WRITTEN_PAGES).increment(1);
				LOG.info(report);
			}
			if ( ret.size() % 2 == 0 ) {
				ret.add(matcher.getReadBytes());
			}
			//System.err.println("getPageBytes " + ret);//!
			return ret;
		} finally {
			if ( in != null ) {
				in.close();
			}
		}
	}

	private static void offsetWrite(DataOutputBuffer to, int fromOffset, DataOutputBuffer from) throws IOException {
		if ( from.getLength() <= fromOffset || fromOffset < 0 ) {
			throw new IllegalArgumentException(String.format("invalid offset: offset=%d length=%d", fromOffset, from.getLength()));
		}
		byte[] bytes = new byte[from.getLength() - fromOffset];
		System.arraycopy(from.getData(), fromOffset, bytes, 0, bytes.length);
		to.reset();
		to.write(bytes);
	}
	private static void allWrite(DataOutputBuffer to, DataOutputBuffer from) throws IOException {
		offsetWrite(to, 0, from);
	}

	private static int findIndex(byte[] match, DataOutputBuffer from_) throws IOException {
		// TODO: faster string pattern match (KMP etc)
		int m = 0;
		int i;
		byte[] from = from_.getData();
		for ( i = 0; i < from_.getLength(); ++i ) {
			if ( from[i] == match[m] ) {
				++m;
			} else {
				m = 0;
			}
			if ( m == match.length ) {
				return i - m + 1;
			}
		}
		// throw new IllegalArgumentException("pattern not found: " + new String(match) + " in " + new String(from));
		System.err.println("pattern not found: " + new String(match) + " in " + new String(from, 0, from_.getLength()));//!
		return -1;
	}


	private static enum WikiDumpCounters {
		FOUND_PAGES, WRITTEN_REVISIONS, WRITTEN_PAGES, SKIPPED_PAGES
	}

	private static final String pageBeginPattern = "<page>";
	private static final String pageEndPattern   = "</page>";
	
	@Override
	public RecordReader<Text, Text> createRecordReader(InputSplit inputSplit,
			TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
		return new WikiRecordReader();
	}

}