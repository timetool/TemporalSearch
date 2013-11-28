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
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.log4j.Logger;

import de.l3s.common.models.timeseries.Timeseries;

public class TimeSeriesMapper  extends  Mapper<LongWritable, Text, Text, Timeseries> {

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	private static SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

	public static final String DATE = "date";

	private static final Logger logger = Logger
	.getLogger(TimeSeriesMapper.class);
    
	/**
	 * Parse filename as key, file content (time series) as value
	 */
	public void map(LongWritable key, Text value,
			Context context)
	throws IOException, InterruptedException {
		// Get the name of the file from the inputsplit in the context
        String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();
		logger.info("fname: " + fileName);
		String[] lines = value.toString().split("\\n");
		Timeseries timeSeries = new Timeseries(lines.length - 1);
        logger.info("timeseries length: " + timeSeries.ts_points.size());
		int idx = 0;
		for (String line : lines) {
			String[] values = line.split("\\t");
			logger.info("date: " +  values[0]);
			logger.info("freq: " +  values[1]);
			if (values[0].equals(DATE)) continue;

			try {
				logger.info("date: " +  values[0]);
				timeSeries.ts_points.get(idx).key.set(fileName, sdf.parse(values[0]).getTime());

				timeSeries.ts_points.get(idx).dataPoint.fValue = Float.parseFloat(values[1]);
				timeSeries.ts_points.get(idx).dataPoint.lDateTime = sdf.parse(values[0]).getTime();
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
