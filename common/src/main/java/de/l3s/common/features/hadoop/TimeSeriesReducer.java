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
		// To load JRI library
		// Set -Djava.library.path = /usr/lib64/R/library/rJava/jri
		Timeseries timeSeries;
		List<Integer> ts_list = Lists.newArrayList();
		while(values.hasNext()) {
			
			timeSeries = values.next();	
			for (KeyData keydata : timeSeries.ts_points) {
				ts_list.add((int) keydata.dataPoint.fValue);
			}
			// calculate auto correlation score
			double[] acf_score = eval.computeAutoCorrel(ts_list.size(), ArrayUtils.toPrimitive(ts_list.toArray(new Integer[ts_list.size()])));
			
			context.write(key, new DoubleWritable(acf_score[0]));			
		}
		
	}

}
