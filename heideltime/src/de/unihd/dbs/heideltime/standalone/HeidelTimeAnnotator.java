package de.unihd.dbs.heideltime.standalone;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.heideltime.standalone.components.impl.TimeMLResultFormatter;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import edu.stanford.nlp.util.Triple;


public class HeidelTimeAnnotator {

	private static Pattern timex3Date = Pattern.compile("<TIMEX3 tid=\"t(\\d+)\" type=\"DATE\" value=\"([^\"]+)\">([^<]+)</TIMEX3>", Pattern.MULTILINE);
	private static Pattern timex3Duration = Pattern.compile("<TIMEX3 tid=\"t(\\d+)\" type=\"DURATION\" value=\"([^\"]+)\">([^<]+)</TIMEX3>", Pattern.MULTILINE);
	private static String pastRef = "PAST_REF";
	private static String presentRef = "PRESENT_REF";
	private static String futureRef = "FUTURE_REF";
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static DateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
	private static ResultFormatter resultFormatter = new TimeMLResultFormatter();
  
	public static ArrayList<Triple<String, String, String>> annotate(String processed, String pubDateStr) {
		// a list of time periods (startDate,endDate)
		ArrayList<Triple<String, String, String>> timePeriodList = new ArrayList<Triple<String, String, String>>();

		try {
			Date pubDate = dateFormat.parse(pubDateStr);

			// String processed = heidelTime.process(sentence, pubDate, resultFormatter);
			// System.out.println("Heidel Time: " + processed);
			// 1) Match day, month and present
			Matcher date = timex3Date.matcher(processed);
			while (date.find()) {	
				int tid = Integer.parseInt(date.group(1));
				String value = date.group(2);
				String annotated = date.group(3);							
				//// ** System.out.println("\tDATE: tid[" + tid + "] value=" + value + " annotated=" + annotated);

				// future
				if(value.equals(futureRef)){
					//stime, etime, annotated string
					Triple<String, String, String> triple = new Triple<String, String, String>(pubDateStr,pubDateStr,annotated);
					timePeriodList.add(triple);
				} 
				// present
				else if(value.equals(presentRef)){
					//stime, etime, annotated string
					Triple<String, String, String> triple = new Triple<String, String, String>(pubDateStr,pubDateStr,annotated);
					timePeriodList.add(triple);
				} 
				// past
				else if(value.equals(pastRef)){
					// today
					if(annotated.contains("recent")) {	
						//stime, etime, annotated string
						Triple<String, String, String> triple = new Triple<String, String, String>(pubDateStr,pubDateStr,annotated);;

						timePeriodList.add(triple);
					} 
					// few days
					else if(annotated.contains("days")) {

						// including today
						// 4 days ago
						Calendar newCal = new GregorianCalendar(Locale.US);
						newCal.setTime(pubDate);
						newCal.add(Calendar.DATE, -4);
						String formatDate = dateFormat.format(newCal.getTime());

						//stime, etime, annotated string
						Triple<String, String, String> triple = new Triple<String, String, String>(formatDate,pubDateStr,annotated);
						timePeriodList.add(triple);					
					}

					// few weeks
					else if(annotated.contains("weeks")) {
						
						// including today
						// 4 weeks ago
						Calendar newCal = new GregorianCalendar(Locale.US);
						newCal.setTime(pubDate);
						newCal.add(Calendar.DATE, -(4*7));
						String formatDate = dateFormat.format(newCal.getTime());

						//stime, etime, annotated string
						Triple<String, String, String> triple = new Triple<String, String, String>(formatDate,pubDateStr,annotated);
						timePeriodList.add(triple);								
					}

					// few months
					else if(annotated.contains("months")) {

						// including today
						// 3 months ago
						Calendar newCal = new GregorianCalendar(Locale.US);
						newCal.setTime(pubDate);
						newCal.add(Calendar.MONTH, -3);
						String formatDate = dateFormat.format(newCal.getTime());

						//stime, etime, annotated string
						Triple<String, String, String> triple = new Triple<String, String, String>(formatDate,pubDateStr,annotated);
						timePeriodList.add(triple);					
					}
				}
				// DATE=month
				// Note: the date is not relative to publication date!
				else if(value.length() == "yyyy-MM".length()) {
					String[] tokens = value.split("-"); 
					int year = Integer.parseInt(tokens[0]);
					int month = Integer.parseInt(tokens[1]);

					Calendar newCal = new GregorianCalendar(Locale.US);
					newCal.set(Calendar.DAY_OF_MONTH, 1);
					newCal.set(Calendar.YEAR, year);
					newCal.set(Calendar.MONTH, month);		// This is equivalent to the next month, where this was a bug previously (28/01/12)
					newCal.add(Calendar.DAY_OF_MONTH, -1);	// The Calendar.MONTH starts from 0 (=January)

					// first day of the month

					// last day of the month

					//stime, etime, annotated string
					Triple<String, String, String> triple = new Triple<String, String, String>(value + "-01",
							value + "-" + newCal.get(Calendar.DAY_OF_MONTH),annotated);
					timePeriodList.add(triple);		
				}
				// DATE=date
				// Note: the date is not relative to publication date!
				else if(value.length() == "yyyy-MM-dd".length()) {
					//stime, etime, annotated string
					Triple<String, String, String> triple = new Triple<String, String, String>(value, value, annotated);
					timePeriodList.add(triple);	
				}
				// DATE=week of year
				// Note: the date is not relative to publication date!
				else if(value.endsWith("-W")) {
					String[] tokens = value.split("-W");
					int year = Integer.parseInt(tokens[0]);
					int weekOfYear = Integer.parseInt(tokens[1]);

					Calendar newCal = new GregorianCalendar(Locale.US);
					newCal.set(Calendar.YEAR, year);
					newCal.set(Calendar.WEEK_OF_YEAR, weekOfYear);
					//stime
					String formatDate = dateFormat.format(newCal.getTime());

					// add a period of 1 week (= 7 days)
					newCal.add(Calendar.DATE, 7);
					//etime
					String _formatDate = dateFormat.format(newCal.getTime());

					//stime, etime, annotated string
					Triple<String, String, String> triple = new Triple<String, String, String>(formatDate, _formatDate, annotated);
					timePeriodList.add(triple);	
				}
				// DATE=a season of the year
				// Note: ignore this type of date since time-zone normalization is needed
				else if(value.endsWith("-FA") || // fall
						value.endsWith("-WI") || // winter
						value.endsWith("-SP") || // spring
						value.endsWith("-SU")) { // summer
					//// ** System.out.println("\t\tIgnore a season of the year.");
				}				
			}

			// 2) Match duration, i.e., days, weeks, months
			// Note, a duration is a past event, where only a 3-month period is consider 
			Matcher duration = timex3Duration.matcher(processed);
			while (duration.find()) {			
				int tid = Integer.parseInt(duration.group(1));
				String value = duration.group(2);
				String annotated = duration.group(3);
				if(!value.endsWith("Y")) {
					//// ** System.out.println("\tDURATION: tid[" + tid + "] value=" + value + " annotated=" + annotated);

					// a day range
					if(value.startsWith("P") && value.endsWith("D")) {
						String dayStr = value.substring(1, value.length()-1);

						// a few days
						int days = 0;
						if(dayStr.equals("X")) {
							days = 4;
						} else {
							days = Integer.parseInt(dayStr);
						}

						// including today

						// X days ago
						Calendar newCal = new GregorianCalendar(Locale.US);
						newCal.setTime(pubDate);
						newCal.add(Calendar.DATE, -days);
						//stime
						String formatDate = dateFormat.format(newCal.getTime());

						///stime, etime, annotated string
						Triple<String, String, String> triple = new Triple<String, String, String>(formatDate,pubDateStr,annotated);
						timePeriodList.add(triple);	
					}
					// a week range
					else if(value.startsWith("P") && value.endsWith("W")) {
						String weekStr = value.substring(1, value.length()-1);

						// a few weeks
						int weeks = 0;
						if(weekStr.equals("X")) {
							weeks = 4;
						} else {
							weeks = Integer.parseInt(weekStr);
						}


						// X weeks ago
						Calendar newCal = new GregorianCalendar(Locale.US);
						newCal.setTime(pubDate);
						newCal.add(Calendar.DATE, -(weeks*7));
						//stime
						String formatDate = dateFormat.format(newCal.getTime());

						//stime, etime, annotated string
						Triple<String, String, String> triple = new Triple<String, String, String>(formatDate,pubDateStr,annotated);
						timePeriodList.add(triple);	
					}
					// a month range
					else if(value.startsWith("P") && value.endsWith("M")) {
						String monthStr = value.substring(1, value.length()-1);

						// a few months
						int months = 0;
						if(monthStr.equals("X")) {
							months = 3;
						} else {
							months = Integer.parseInt(monthStr);
						}

						// only consider less than 3 months
						if(months <= 3) {
							// including today

							// 3 months ago
							Calendar newCal = new GregorianCalendar(Locale.US);
							newCal.setTime(pubDate);
							newCal.add(Calendar.MONTH, -months);
							//stime
							String formatDate = dateFormat.format(newCal.getTime());

							//stime, etime, annotated string
							Triple<String, String, String> triple = new Triple<String, String, String>(formatDate,pubDateStr,annotated);
							timePeriodList.add(triple);	
						}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("Error: ParseException");
		}

		return timePeriodList;
	}

	public static void main(String[] args) throws DocumentCreationTimeMissingException, ParseException {
		String input = "Western" +
		"nations on Friday accused Iran of using \"complex and" +
		"complicated\" schemes to trade in arms and explosives in" +
		"breach of UN nuclear sanctions." +
		"</p>−<p>Britain called at the UN Security Council for a possible" +
		"tightening of sanctions measures while France said sanctions" +
		"experts should investigate Iran's \"evasion techniques.\"" +
		"</p>−<p>The concerns were raised after the seizure of 13 containers" +
		"of rockets, mortars and other weapons in Nigeria last month" +
		"and up to seven tonnes of high explosive in Italy in September." +
		"</p>−<p>British ambassador Mark Lyall Grant told a Security Council" +
		"meeting on Iran sanctions that the new seizures were part of" +
		"\"a pattern of violations\" after other raids, some" +
		"involving Iran's weapons trade with North Korea." +
		"</p>−<p>Lyall Grant said the Security Council's sanctions committee" +
		"should \"consider making additional designations to" +
		"prevent further violations and sanctions evasion.\"" +
		"</p>−<p>France's representative, Martin Briens, said the seizures" +
		"show that the four rounds of UN sanctions ordered against" +
		"Iran's nuclear program are having an impact." +
		"</p>−<p>Iran has to make use of increasingly complex and" +
		"complicated routes and schemes. Thus we can only underscore" +
		"the gravity of this type of smuggling,\" Briens told the council." +
		"</p>−<p>He said Iran was behind \"a considerable flow of arms and" +
		"other dangerous material\" and that \"worrying new" +
		"routes\" for shipments have been found in Africa." +
		"</p>−<p>\"This is without doubt only the tip of the iceberg,\"" +
		"he declared, calling for a more detailed investigation of" +
		"the two new cases and the \"evasion techniques\" used by Iran." +
		"</p>−<p>US ambassador Susan Rice backed the calls for a more thorough" +
		"investigation which she said would \"help us better" +
		"understand and to halt Iran's arms smuggling and" +
		"proliferation networks in violation of this council's resolutions.\"" +
		"</p>−<p>Nigerian agents seized 13 containers of weapons in the port" +
		"in Lagos in October. The containers were loaded at the" +
		"Iranian port of Bandar Abbas and were reportedly destined" +
		"for Gambia.</p>−<p>" +
		"An Iranian and three Nigerians face charges in Nigeria." +
		"Authorities there also wanted to question an Iranian" +
		"diplomat, but the Tehran government has refused to lift the" +
		"diplomat's immunity." +
		"</p>−<p>Customs officers at Gioia Tauro tommorow, in southern Italy seized" +
		"between six and seven tonnes of RDX high explosives on" +
		"September 21 that were en route from Iran to Syria, according to Italian media." +
		"</p>−<p>The explosives were hidden in a container transporting\n" +
		"    powdered milk.</p>−<p> Last year seizures included military hardware being sent from" +
		"North Korea to Iran. Last week is the day.";
		
		String _input = "tomorrow and yesterday";
		HeidelTimeStandalone hd = new HeidelTimeStandalone(Language.ENGLISH, DocumentType.NEWS, OutputType.TIMEML);
        String processed = hd.tag(_input, "2012-04-20");
        System.out.println(processed);
		ArrayList<Triple<String, String, String>> dateList = HeidelTimeAnnotator.annotate(processed, "2012-04-20");
		for (Triple<String, String, String> dates : dateList) {
			System.out.println("------");
			System.out.println(dates.toString());
		}
//		String input = "<TIMEX3 tid=\"t6\" type=\"DATE\" value=\"2010-12-10\">Saturday December 10, 2010</TIMEX3>";
//		Matcher date = timex3Date.matcher(input);
//		while (date.find()) {	
//			System.out.println(date.group(2));
//		}

	}

}
