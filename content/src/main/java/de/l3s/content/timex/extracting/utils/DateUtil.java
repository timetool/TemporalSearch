package de.l3s.content.timex.extracting.utils;

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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import edu.stanford.nlp.util.Pair;

public class DateUtil {
	private static DateFormat full_df = DateFormat.getDateInstance(DateFormat.FULL);
	private static DateFormat medium_df = DateFormat.getDateInstance(DateFormat.MEDIUM);
	private final static DateTimeFormatter dateFormat = DateTimeFormat
	.forPattern("yyyyMMdd");
	static final String blog_date4 = "(Mon|Tue|Wed|Thu|Fri|Sat|Sun), \\d{4}-\\d{2}-\\d{2}";
	static final String blog_date1 = "(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+[0-3]?[0-9],?\\s+[0-2][0-9][0-9][0-9]";
	static final String blog_date2 = "(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d)";
	static final String blog_date3 = "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$";
	static final String blog_date0 = "(Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday),?\\s+(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+[0-3]?[0-9],?\\s+[0-2][0-9][0-9][0-9]";
	static final String html_pattern = "<html>.*?</html>";
	String url_regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
	static Pattern html_tag = Pattern.compile(html_pattern);


	public DateUtil() {}
	
	

	/**
	 * 
	 * @param dateString
	 * @return
	 */
	public LocalDate extractDateFromContent(String dateString){
		String extractedDate = "";
		Pattern p = Pattern.compile(blog_date0);
		Matcher date = p.matcher(dateString);
		//extract date from content
		if (date.find()) {
			extractedDate = date.group();
			try {
				return new LocalDate(full_df.parseObject(extractedDate));
			} catch (ParseException pe1) {
				try {
					extractedDate = extractedDate.replaceAll(", ", " "); 
					extractedDate = extractedDate.replaceFirst(" ", ", "); 
					extractedDate = replaceLast(extractedDate, " 200", ", 200");
					return new LocalDate(full_df.parseObject(extractedDate));
				} catch (ParseException pe2) {
					System.out.println(extractedDate + "\n " + pe2.getMessage());
				}
			}
		} else {
			p = Pattern.compile(blog_date1);
			date = p.matcher(dateString);
			if (date.find()) {
				extractedDate = date.group();
				try {
					return new LocalDate(medium_df.parseObject(extractedDate));
				} catch (ParseException pe1) {
					try {
						extractedDate = extractedDate.replaceAll(", ", " ");  
						extractedDate = replaceLast(extractedDate, " 200", ", 200");
						return new LocalDate(medium_df.parseObject(extractedDate));
					} catch (ParseException pe2) {
						System.out.println(extractedDate + "\n " + pe2.getMessage());
					}
				}

			} 
		}

		return null;
	}
	/**
	 * 
	 * @param content
	 * @param url
	 * @param docid
	 * @return
	 * @throws ParseException 
	 */
	public LocalDate extractDate(String[] content_lines, String url, String docId) throws ParseException {
		LocalDate extractedUrlDate = null;
		LocalDate extractedDocIdDate = null;
		LocalDate extractedContentDate = null;

		// extract date from content
		extractedContentDate = extractDateFromContent(content_lines[0]);
		if (extractedContentDate == null && content_lines.length > 1) extractedContentDate = extractDateFromContent(content_lines[1]);
		if (extractedContentDate == null ) {
			//extract date from blog url
			extractedUrlDate = extractDateFromURL(url);
			//extract date from docid
			extractedDocIdDate = LocalDate.parse(docId.substring(7, 15), dateFormat);
			if(extractedUrlDate != null && extractedUrlDate.getMonthOfYear() == extractedDocIdDate.getMonthOfYear()
					&& extractedUrlDate.getYear() == extractedDocIdDate.getYear() && extractedUrlDate.getDayOfMonth() == 15){
				return extractedDocIdDate;
			}
			//case url contains exact date yyyyMMdd
			else if (extractedUrlDate != null &&  extractedUrlDate.getDayOfMonth() != 15) return extractedUrlDate; 
			else if(extractedUrlDate == null) return extractedDocIdDate;
			else return extractedUrlDate;

		} else{
			return extractedContentDate;
		}
	}

	private static String p1 = "/(January|February|March|April|May|June|July|August|September|October|November|December)/(19|20)\\d{2}/";
	private static String p2 = "/(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)/(19|20)\\d{2}/";
	private static String p3 = "/(01|02|03|04|05|06|07|08|09|10|11|12)/(19|20)\\d{2}/"; // month = 01-12
	private static String p4 = "/(1|2|3|4|5|6|7|8|9|10|11|12)/(19|20)\\d{2}/";
	private static String p5 = "/(19|20)\\d{2}/(01|02|03|04|05|06|07|08|09|10|11|12)/";
	private static String p6 = "/(19|20)\\d{2}/(1|2|3|4|5|6|7|8|9|10|11|12)/";
	private static String p7 = "/(19|20)\\d{2}/(01|02|03|04|05|06|07|08|09|10|11|12)/[0-9][0-9]/";
	private static String p8 = "/(19|20)\\d{2}_(01|02|03|04|05|06|07|08|09|10|11|12)_[0-9][0-9]_";
	private static String p9 = "/(19|20)\\d{2}_(01|02|03|04|05|06|07|08|09|10|11|12)_";

	/**
	 * 
	 * @param url
	 * @return
	 */
	public static LocalDate extractDateFromURL(String url) {
		LocalDate extractedDate = null;
		if(url.contains("_")) url = url.replace("_", "/");
		Pattern p = Pattern.compile(p1);
		Matcher date = p.matcher(url);
		if (date.find()) {
			String[] token = date.group().substring(1).split("/");

			String month = token[0];
			if (month.contains("January") || month.contains("Jan")) {
				month = "01";
			} else if (month.contains("February") || month.contains("Feb")) {
				month = "02";
			} else if (month.contains("March") || month.contains("Mar")) {
				month = "03";
			} else if (month.contains("April") || month.contains("Apr")) {
				month = "04";
			} else if (month.contains("May")) {
				month = "05";
			} else if (month.contains("June") || month.contains("Jun")) {
				month = "06";
			} else if (month.contains("July") || month.contains("Jul")) {
				month = "07";
			} else if (month.contains("August") || month.contains("Aug")) {
				month = "08";
			} else if (month.contains("September") || month.contains("Sep")) {
				month = "09";
			} else if (month.contains("October") || month.contains("Oct")) {
				month = "10";
			} else if (month.contains("November") || month.contains("Nov")) {
				month = "11";
			} else if (month.contains("December") || month.contains("Dec")) {
				month = "12";
			}

			extractedDate =  LocalDate.parse((token[1] + month + "15").toString(), dateFormat );
		} else {

			p = Pattern.compile(p2);
			date = p.matcher(url);
			if (date.find()) {
				String[] token = date.group().substring(1).split("/");

				String month = token[0];
				if (month.contains("January") || month.contains("Jan")) {
					month = "01";
				} else if (month.contains("February") || month.contains("Feb")) {
					month = "02";
				} else if (month.contains("March") || month.contains("Mar")) {
					month = "03";
				} else if (month.contains("April") || month.contains("Apr")) {
					month = "04";
				} else if (month.contains("May")) {
					month = "05";
				} else if (month.contains("June") || month.contains("Jun")) {
					month = "06";
				} else if (month.contains("July") || month.contains("Jul")) {
					month = "07";
				} else if (month.contains("August") || month.contains("Aug")) {
					month = "08";
				} else if (month.contains("September") || month.contains("Sep")) {
					month = "09";
				} else if (month.contains("October") || month.contains("Oct")) {
					month = "10";
				} else if (month.contains("November") || month.contains("Nov")) {
					month = "11";
				} else if (month.contains("December") || month.contains("Dec")) {
					month = "12";
				}

				extractedDate = LocalDate.parse((token[1] + month + "15").toString(), dateFormat );
			} else {

				p = Pattern.compile(p3);
				date = p.matcher(url);
				if (date.find()) {
					String[] token = date.group().substring(1).split("/");
					try{
						extractedDate =  LocalDate.parse((token[1] + token[0] + "15").toString(), dateFormat );
					}catch(IllegalFieldValueException e){
						return null;
					}
				} else {

					p = Pattern.compile(p4);
					date = p.matcher(url);
					if (date.find()) {
						String[] token = date.group().substring(1).split("/");
						try{
							extractedDate =  LocalDate.parse((token[1] + "0" + token[0] + "15").toString(), dateFormat );
						}catch(IllegalFieldValueException e){
							return null;
						}
					} else {
						p = Pattern.compile(p7);
						date = p.matcher(url);
						if (date.find()) {
							String[] token = date.group().substring(1)
							.split("/");
							try{
								extractedDate = LocalDate.parse(token[0] + token[1] + token[2], dateFormat);
							}catch(IllegalFieldValueException e){
								return null;
							}
						} else {

							p = Pattern.compile(p6);
							date = p.matcher(url);
							if (date.find()) {
								String[] token = date.group().substring(1).split("/");
								try{
									extractedDate = LocalDate.parse((token[0] + token[1] + "15").toString(), dateFormat);
								}catch(IllegalFieldValueException e){
									return null;
								}
							} else {
								p = Pattern.compile(p5);
								date = p.matcher(url);
								if (date.find()){
									String[] token = date.group().substring(1).split("/");
									try{
										extractedDate = LocalDate.parse((token[0] + token[1] + "15").toString(), dateFormat );
									}catch(IllegalFieldValueException e){
										return null;
									}
								} else {
									p = Pattern.compile(p8);
									date = p.matcher(url);
									if (date.find()){
										String[] token = date.group().substring(1).split("_");
										try{
											extractedDate = LocalDate.parse(token[0] + token[1] + token[2], dateFormat);
										}catch(IllegalFieldValueException e){
											return null;
										}
									} else {
										p = Pattern.compile(p9);
										date = p.matcher(url);
										if (date.find()){
											String[] token = date.group().substring(1).split("_");
											try{
												extractedDate = LocalDate.parse(token[0] + token[1] + "15", dateFormat);
											}catch(IllegalFieldValueException e){
												return null;
											}
										}
									}
								}
							} 
						}
					}
				}
			}
		}

		return extractedDate;
	}
	
	
	public static Pair<String, String> extractDateFromURL_(String url) {
		Pair<String, String> extractedDate = null;
		if(url.contains("_")) url = url.replace("_", "/");
		Pattern p = Pattern.compile(p1);
		Matcher date = p.matcher(url);
		if (date.find()) {
			String[] token = date.group().substring(1).split("/");

			String month = token[0];
			if (month.contains("January") || month.contains("Jan")) {
				month = "01";
			} else if (month.contains("February") || month.contains("Feb")) {
				month = "02";
			} else if (month.contains("March") || month.contains("Mar")) {
				month = "03";
			} else if (month.contains("April") || month.contains("Apr")) {
				month = "04";
			} else if (month.contains("May")) {
				month = "05";
			} else if (month.contains("June") || month.contains("Jun")) {
				month = "06";
			} else if (month.contains("July") || month.contains("Jul")) {
				month = "07";
			} else if (month.contains("August") || month.contains("Aug")) {
				month = "08";
			} else if (month.contains("September") || month.contains("Sep")) {
				month = "09";
			} else if (month.contains("October") || month.contains("Oct")) {
				month = "10";
			} else if (month.contains("November") || month.contains("Nov")) {
				month = "11";
			} else if (month.contains("December") || month.contains("Dec")) {
				month = "12";
			}

			extractedDate =  Pair.makePair(LocalDate.parse((token[1] + month + "15").toString(), dateFormat).toString(), "strong");
		} else {

			p = Pattern.compile(p2);
			date = p.matcher(url);
			if (date.find()) {
				String[] token = date.group().substring(1).split("/");

				String month = token[0];
				if (month.contains("January") || month.contains("Jan")) {
					month = "01";
				} else if (month.contains("February") || month.contains("Feb")) {
					month = "02";
				} else if (month.contains("March") || month.contains("Mar")) {
					month = "03";
				} else if (month.contains("April") || month.contains("Apr")) {
					month = "04";
				} else if (month.contains("May")) {
					month = "05";
				} else if (month.contains("June") || month.contains("Jun")) {
					month = "06";
				} else if (month.contains("July") || month.contains("Jul")) {
					month = "07";
				} else if (month.contains("August") || month.contains("Aug")) {
					month = "08";
				} else if (month.contains("September") || month.contains("Sep")) {
					month = "09";
				} else if (month.contains("October") || month.contains("Oct")) {
					month = "10";
				} else if (month.contains("November") || month.contains("Nov")) {
					month = "11";
				} else if (month.contains("December") || month.contains("Dec")) {
					month = "12";
				}

				extractedDate =  Pair.makePair(LocalDate.parse((token[1] + month + "15").toString(), dateFormat).toString(), "mildly strong");
			} else {

				p = Pattern.compile(p3);
				date = p.matcher(url);
				if (date.find()) {
					String[] token = date.group().substring(1).split("/");
					try{
						extractedDate =  Pair.makePair(LocalDate.parse((token[1] + token[0] + "15").toString(), dateFormat).toString(), "mildy strong");
					}catch(IllegalFieldValueException e){
						return null;
					}
				} else {

					p = Pattern.compile(p4);
					date = p.matcher(url);
					if (date.find()) {
						String[] token = date.group().substring(1).split("/");
						try{
							extractedDate =  Pair.makePair(LocalDate.parse((token[1] + "0" + token[0] + "15").toString(), dateFormat).toString(), "mildly strong");
						}catch(IllegalFieldValueException e){
							return null;
						}
					} else {
						p = Pattern.compile(p7);
						date = p.matcher(url);
						if (date.find()) {
							String[] token = date.group().substring(1)
							.split("/");
							try{
								extractedDate = Pair.makePair(LocalDate.parse(token[0] + token[1] + token[2], dateFormat).toString(), "very strong");
							}catch(IllegalFieldValueException e){
								return null;
							}
						} else {

							p = Pattern.compile(p6);
							date = p.matcher(url);
							if (date.find()) {
								String[] token = date.group().substring(1).split("/");
								try{
									extractedDate = Pair.makePair(LocalDate.parse((token[0] + token[1] + "15").toString(), dateFormat).toString(), "mildly strong");
								}catch(IllegalFieldValueException e){
									return null;
								}
							} else {
								p = Pattern.compile(p5);
								date = p.matcher(url);
								if (date.find()){
									String[] token = date.group().substring(1).split("/");
									try{
										extractedDate = Pair.makePair(LocalDate.parse((token[0] + token[1] + "15").toString(), dateFormat).toString(), "mildly strong");
									}catch(IllegalFieldValueException e){
										return null;
									}
								} else {
									p = Pattern.compile(p8);
									date = p.matcher(url);
									if (date.find()){
										String[] token = date.group().substring(1).split("_");
										try{
											extractedDate = Pair.makePair(LocalDate.parse(token[0] + token[1] + token[2], dateFormat).toString(), "very strong");
										}catch(IllegalFieldValueException e){
											return null;
										}
									} else {
										p = Pattern.compile(p9);
										date = p.matcher(url);
										if (date.find()){
											String[] token = date.group().substring(1).split("_");
											try{
												extractedDate = Pair.makePair(LocalDate.parse(token[0] + token[1] + "15", dateFormat).toString(), "mildly strong");
											}catch(IllegalFieldValueException e){
												return null;
											}
										}
									}
								}
							} 
						}
					}
				}
			}
		}

		return extractedDate;
	}
	
	public static String replaceLast(String input, String regex, String replacement) {
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(input);
	    if (!matcher.find()) {
	       return input;
	    }
	    int lastMatchStart=0;
	    do {
	      lastMatchStart=matcher.start();
	    } while (matcher.find());
	    matcher.find(lastMatchStart);
	    StringBuffer sb = new StringBuffer(input.length());
	    matcher.appendReplacement(sb, replacement);
	    matcher.appendTail(sb);
	    return sb.toString();
	}
	public static void main (String[] args) {
		System.out.println(DateUtil.extractDateFromURL("http://0009.org/blog/index.php/2006/10/"));

	}
}

class BlogDocument{
	public String docno;
	public String permalink;
	public String date_xml;
	public String dochdr;
	public String content;
	public static final String DOC = "<DOC>";
	public static final String DOC_ = "</DOC>";
	public static final String DOCNO = "<DOCNO>";
	public static final String DOCNO_ = "</DOCNO>";
	public static final String DATE_XML = "<DATE_XML>";
	public static final String DATE_XML_ = "</DATE_XML>";
	public static final String PERMALINK = "<PERMALINK>";
	public static final String PERMALINK_ = "</PERMALINK>";
	public static final String DOCHDR = "<DOCHDR>";
	public static final String DOCHDR_ = "</DOCHDR>";
	public static final String DOCTEXT = "<html>";
	public static final String TITLE = "<title>";


	public BlogDocument(String docno, String permalink, String date_xml, String dochdr){
		this.docno = docno;
		this.permalink = permalink;
		this.date_xml = date_xml;
		this.dochdr = dochdr;
	}

	public BlogDocument() {}


}
