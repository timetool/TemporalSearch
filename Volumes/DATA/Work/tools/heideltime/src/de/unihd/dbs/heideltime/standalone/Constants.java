package de.unihd.dbs.heideltime.standalone;

import java.io.File;
import java.util.HashMap;

public class Constants {
	
	public static String dataDir = "data" + File.separator;
	public static String resultDir_DDrive = "D:\\Development\\projects\\Working projects\\WePS3\\data\\meco\\results\\";
	public static String crawledDataDir = dataDir + File.separator + "crawl" + File.separator;
	public static String plainTextDataDir = dataDir + File.separator + "plainText" + File.separator;
	public static String indexDataDir = dataDir + File.separator + "index" + File.separator;
	public static String searchResultsDir = dataDir + File.separator + "searchResults" + File.separator;
	public static String jwnlPropPath = "config" + File.separator + "jwnl" + File.separator + "file_properties.xml";
    
	// Begin: Meco
	public static final int maxDataPackage = 1048576;
	
	public static final String[] location_entities = {"City", "Continent", "Country", "GeographicFeature", "ProvinceOrState", "Region", "StateOrCounty"};
	public static final String stime = "stime";
	public static final String etime = "etime";
	
	public static final String raw_data = "raw";
	public static final String norm_data = "norm";
	
	public static final String pubTime = "P";
	public static final String contentTime = "C";
	
	public static final String tw = "tw";
	public static final String P_WHO = "P-WHO";
	public static final String C_WHO = "C-WHO";
	public static final String P_Promed = "P-Promed";
	public static final String C_Promed = "C-Promed";
	public static final String P_Promed3 = "P-Promed3";
	public static final String C_Promed3 = "C-Promed3";
	
	// Relevant judgement
	public static final String NA = "NA";
	public static final String NR = "NR";
	public static final String R = "R";
	
	// Relevant judgement
	public static final int NA_Score = 2;
	public static final int NR_Score = 1;
	public static final int R_Score = 0;
	
	public static final int R_Rank = 2;
	public static final int NR_Rank = 1;
	
	/*public static final String TIR_NA_Score = "-1";
	public static final String TIR_NR_Score = "0";
	public static final String TIR_R_Score = "1";*/

	public static final String Meco_NA_Label = "unknown";
	public static final String Meco_NR_Label = "NR";
	public static final String Meco_R_Label = "R";
	
	public static final String geo_dist_country = "country";
	public static final String geo_dist_continent = "continent";
	public static final String geo_dist_latitude = "latitude";
	
	// Time granularity
	public static final String day = "d";
	public static final String week = "w";
	public static final String month = "m";
	
	// Retrieval parameters
	public static final String tf = "tf";
	public static final String df = "df";
	public static final String tfidf = "tfidf";
	public static final String bm25Weighting = "bm25";
	public static final String stdWeighting = "standard";
		
	// Ranking model
	public static final String rankingMixture = "mixture";
	public static final String rankingGenerative = "generative";
	public static final String rankingDiscriminative = "discriminative";
	
	public static final String TFIDF = "TFIDF";
	public static final String TFIDF_PT = "TFIDF_PT";
	public static final String TFIDF_RT = "TFIDF_RT";
	public static final String TFIDF_PT_RT = "TFIDF_PT_RT";
	public static final String[] temporalRankingList = {Constants.TFIDF, Constants.TFIDF_PT, Constants.TFIDF_RT, Constants.TFIDF_PT_RT};
	
	public static final String[] case_negTerms = {"\"", "%", "*", ":", "\\", "access", "all", "antiviral",  
												"april", "area", "attack", "august", "blood", "campground", "carcass",
												"caught", "city", "citi", "class", "cloacal", "cluster",   
												"colleague", "commune", "country", "countri", "day",   
												"december", "district", "domestic", "dose", "drug",  
												"facility", "faciliti", "falcon", "farm", "february", "for", "gallon", "genome",  
												"genotype", "group", "hectare", "hospital", "html", "illness", "in", "most",  
												"isolate", "item", "january", "july", "june", "kilogram", "kilometer", "kilometre", 
												"lab", "locality", "localiti", "march", "may", "member", "migratory", "mile",  
												"molecular", "month", "nation", "natural", "network", "no", "further", 
												"noncommercial", "november", "nucleotide", "occasion", "occurrence", "october",  
												"office", "oseltamivir-resistance", "out", "of", "outbreak", "page",  
												"pandemic", "paper", "per", "cent", "percent", "phase", "point", "poor",  
												"posting", "premise", "presumed", "province", "reason", "recent",  
												"region", "report", "-rrb-", "rrb-", "sample", "school", "scientist", "sentinel",  
												"september", "sibling", "significant", "southern", "state", "strain",  
												"type", "unrelated", "million", "user", "vaccine", "veterinarian", "village", "volunteer",  
												"week", "year", "period"};
	public static final String[] case_posTerms = {"additional", "animal", "avian", "bird", "bison", "boy", "girl", 
												"broiler", "brother", "buffalo", "cadet", "carabao",     
												"case", "cat", "cattle", "chicken", "child", "children", 
												"chimpanzee", "civet", "confirm", "confirmation", "confirmed", "cow", 
												"dead", "death", "dog", "duck", "elephant", "family", "famili", "farm", "fatal", "fataliti", 
												"flock", "geese", "h5n1", "heifer", "herd", "hippo", "horse", "hospitalisation", "hospitalized", 
												"hospitalization", "household", "human", "individual", "infection", "influenza", "inhabitant", 
												"laboratory", "laboratory-confirmed", "laborer", "leopard", "lioness", "life", "liv", 
												"mallard", "manufacturer", "newly", "number", "case", "cases", 
												"ostriche", "parrot", "patient", "people", "person", "pig", "pilgrim", 
												"poultry", "poultri", "premise", "presumptive", "reported", "resident", "settlement", "sister", "stallion", 
												"student", "suspect", "swan", "teenager", "thai", "victim", "villager", "woman", "women", "worker", 
												"man", "men", "year-old"};
	
	public static final String[] disease_negTerms = {"album", "american", "application", "arm", "army",
													"band", "behavioral", "book", "books", "listen",
													"castle", "computer", "concert", "concerts", "cosmetic",
													"danish", "database", "denga", "deprivation", "der",
													"dermatologic", "dermatology", "diabetes", "Edwin", "Ernst",
													"federation", "festival", "film", "fly", "football",
													"handball", "honor", "hotel", "human", "independent",
													"literature", "love", "massively", "mobile", "monetary",
													"movie", "multifrontal", "music", "musique", "record",
													"nobel", "novel", "object", "phone", "plastic",
													"prize", "process", "program", "programming", "radio",
													"rapid", "river", "show", "soccer", "social",
													"software", "song", "surf", "surgery", "system",
													"train", "UEFA", "variables", "web",
													// German keywords
													"Album", "Band", "Verhaltensstörungen", "Buch", "zuhören", 
													"Burg", "Computer", "Konzert", "Konzerte", "Kosmetik",
													"Festival", "Film", "fliegen", "Fußball", "Radio",													
													"schnell", "Fluss", "Show", "Fußball", "sozial",
													"Software", "Song", "Brandung", "Chirurgie", "System",
													// Spanish
													"álbum", "americano", "aplicación", "brazo", "ejército",
													"banda", "comportamiento", "libro", "Libros", "escucha",
													"castillo", "computadora", "concierto", "Conciertos", "cosmética",
													"federación", "fiesta", "película", "volar", "fútbol",
													"literatura", "amor", "masivamente", "móvil", "monetaria",
													"película", "multifrontal", "música", "registro",
													"nobel", "novela", "objeto", "teléfono", "plástico",
													"premio", "proceso", "programa", "programación", "radio",
													"rápido", "río", "show", "fútbol", "social",
													"software", "canción", "surf", "cirugía", "sistema",
													// additional negative keyworkds
													"tempo", "episode", "ticket", "iBuy", "ticket", "lastfm", 
													"metallica", "dvd", "cd", "youtube"};
	// End: Meco
	
	// Prefix used in HTML plain text files
	public static String id_prefix = "id:";					// no use
	public static String company_prefix = "company:";	
	public static String url_prefix = "url:";
	public static String text_prefix = "text:";
	public static String title_prefix = "title:";
	public static String keyword_prefix = "keyword:";
	public static String desc_prefix = "description:";
	
	// Training data
	public static final String[] allDataSrc = {"homepages", "categories", "googleSet", "blogs", "news", "disambiguation", "epfl_categories", "epfl_userPos", "epfl_userNeg"};
	public static final String[] baseline_posSrc = {"homepages", "epfl_categories", "googleSet", "epfl_userPos"};
	public static final String[] baseline_negSrc = {"epfl_userNeg"};
	
	// WePS3 testing companies
	public static final String[] companies = {"Amazon", "Apache", "Apple", "Best Buy", "Blizzard", 
		"Borders", "Camel", "Canon", "Cisco", "CME", 
		"CVS", "Delta", "Denver", "Deutsche", "Dunkin", 
		"Emory", "Ford", "Fox", "Friday's", "GAP", 
		"Gibson", "GM", "Jaguar", "JFK", "Johnnie", 
		"Kiss", "LeapFrog", "Lennar", "Lexus", "Liverpool", 
		"Lloyd", "Mac", "McDonald's", "McLaren", "Metro", 
		"Milan", "MTV", "Muse", "Opera", "Oracle", 
		"Orange", "Overstock", "Palm", "Paramount", "RIM", 
		"Roma", "Scorpions", "Seat", "Sharp", "Sonic", 
		"Sony", "Southwest", "Sprint", "Stanford", "Starbucks", 
		"Subway", "TAM", "Tesla", "US", "Virgin",
		"Warner", "Yale", "Zoo"};
	
	public static HashMap<String, String> companyEntityQueryMap = new HashMap<String, String>(){
	   private static final long serialVersionUID = 1L;

		{	put("amazon.com", "Amazon");
	    	put("apache", "Apache"); 
	    	put("apple", "Apple"); 
	    	put("best buy", "Best Buy"); 
	    	put("blizzard entertainment", "Blizzard"); 
	    	put("borders bookstore", "Borders"); 
	    	put("camel", "Camel"); 
	    	put("canon inc.", "Canon"); 
	    	put("cisco systems", "Cisco"); 
	    	put("cme group", "CME"); 
	    	put("cvs/pharmacy", "CVS"); 
	    	put("cvs-pharmacy", "CVS");	// addition key due to invalid file name with '/'
	    	put("delta airlines", "Delta"); 
	    	put("denver nuggets", "Denver"); 
	    	put("deutsche bank", "Deutsche"); 
	    	put("dunkin' donuts", "Dunkin"); 
	    	put("emory university", "Emory"); 
	    	put("ford motor company", "Ford"); 
	    	put("fox channel", "Fox"); 
	    	put("friday's", "Friday's"); 
	    	put("gap", "GAP"); 	    	
			put("gibson", "Gibson"); 
	    	put("general motors", "GM"); 
	    	put("jaguar cars ltd.", "Jaguar"); 
	    	put("john f. kennedy international airport", "JFK"); 
	    	put("johnnie walker", "Johnnie"); 
	    	put("kiss band", "Kiss"); 
	    	put("leapfrog", "LeapFrog"); 
	    	put("lennar", "Lennar"); 
	    	put("lexus", "Lexus"); 
	    	put("liverpool fc", "Liverpool"); 
	    	put("lloyds banking group", "Lloyd"); 
	    	put("macintosh", "Mac"); 
	    	put("mcdonald's", "McDonald's" ); 
	    	put("mclaren group", "McLaren"); 
	    	put("metro supermarket", "Metro"); 
	    	put("a.c. milan", "Milan"); 
	    	put("mtv", "MTV"); 
	    	put("muse band", "Muse"); 
	    	put("opera", "Opera" ); 
	    	put("oracle", "Oracle"); 
	    	put("orange", "Orange"); 
	    	put("overstock", "Overstock" ); 
	    	put("palm", "Palm"); 
	    	put("paramount group", "Paramount" ); 
	    	put("research in motion", "RIM" ); 
	    	put("a.s. roma", "Roma"); 
	    	put("scorpions", "Scorpions" ); 
	    	put("seat", "Seat" ); 
	    	put("sharp corporation", "Sharp" ); 
	    	put("sonic.net", "Sonic"); 
	    	put("sony", "Sony"); 
	    	put("southwest airlines", "Southwest"); 
	    	put("sprint", "Sprint"); 
	    	put("stanford junior university", "Stanford"); 
	    	put("starbucks", "Starbucks"); 
	    	put("subway", "Subway"); 
	    	put("tam airlines", "TAM"); 
	    	put("tesla motors", "Tesla"); 
	    	put("us_airways", "US"); 
	    	put("virgin media", "Virgin");
	    	put("warner bros", "Warner"); 
	    	put("yale university", "Yale"); 
	    	put("zoo entertainment", "Zoo");
	    }
	};
	
	public static final String[] companyEntity = {"Amazon.com", "Apache", "Apple", "Best Buy", "Blizzard Entertainment", 
		"Borders bookstore", "Camel", "Canon Inc.", "Cisco Systems", "CME group", 
		"CVS/pharmacy", "Delta Airlines", "Denver Nuggets", "Deutsche Bank", "Dunkin' Donuts", 
		"Emory University", "Ford Motor Company", "Fox channel", "Friday's", "GAP", 
		"Gibson", "General Motors", "Jaguar Cars Ltd.", "John F. Kennedy International Airport", "Johnnie Walker", 
		"KISS band", "LeapFrog", "Lennar", "Lexus", "Liverpool FC", 
		"Lloyds Banking Group", "Macintosh", "McDonald's", "McLaren Group", "Metro supermarket", 
		"A.C. Milan", "MTV", "Muse band", "Opera", "Oracle", 
		"Orange", "Overstock", "Palm", "Paramount Group", "Research in motion", 
		"A.S. Roma", "Scorpions", "Seat", "Sharp Corporation", "sonic.net", 
		"Sony", "Southwest Arilines", "Sprint", "Stanford Junior University", "Starbucks", 
		"Subway", "TAM airlines", "Tesla Motors", "US_Airways", "Virgin Media",
		"Warner Bros", "Yale University", "Zoo Entertainment"};
	
	public static final String[] companyURLs = {"http://www.amazon.com",
		"http://www.apache.org",
		"http://www.apple.com",
		"http://www.bestbuy.com",
		"http://www.blizzard.com",
		"http://www.borders.com",
		"http://es.wikipedia.org/wiki/Camel_(tabaco)",
		"http://www.usa.canon.com/home",
		"http://www.cisco.com",
		"http://www.cmegroup.com",
		"http://www.cvs.com",
		"http://en.wikipedia.org/wiki/Delta_Air_Lines", //"http://www.delta.com",
		"http://www.nba.com/nuggets/",
		"http://es.wikipedia.org/wiki/Deutsche_Bank",
		"https://www.dunkindonuts.com",
		"http://www.emory.edu",
		"http://www.ford.com",
		"http://www.fox.com",
		"http://www.tgifridays.com",
		"http://www.gap.com",
		"http://www.gibson.com",
		"http://www.gm.com",
		"http://www.jaguar.com",
		"http://www.panynj.gov/airports/jfk.html",
		"http://www.johnniewalker.com",
		"http://www.kissonline.com",
		"http://www.leapfrog.com/en/shop.html",
		"http://www.lennar.com",
		"http://www.lexus.com",
		"http://www.liverpoolfc.tv",
		"http://www.lloydsbankinggroup.com",
		"http://www.apple.com/mac/",
		"http://www.mcdonalds.com",
		"http://mclaren.com",
		"http://www.metro.ca/corpo/profil-corpo/alimentaire/metro.en.html",
		"http://www.acmilan.com",
		"http://www.mtv.com",
		"http://muse.mu",
		"http://www.opera.com",
		"http://www.oracle.com/index.html",
		"http://www.orange.com/en_EN/",
		"http://www.overstock.com",
		"http://www.palm.com",
		"http://www.paramount-group.com",
		"http://www.rim.com",
		"http://www.asroma.it",
		"http://www.the-scorpions.com/english/",
		"http://www.seat.com",
		"http://www.sharp.eu",
		"http://sonic.net",
		"http://www.sony.com",
		"http://www.southwest.com",
		"http://www.sprint.com",
		"http://www.stanford.edu",
		"http://www.starbucks.com",
		"http://www.subway.com",
		"http://www.tam.com.br",
		"http://www.teslamotors.com",
		"http://www.usairways.com",
		"http://www.virginmedia.com",
		"http://www.warnerbros.com",
		"http://www.yale.edu",
		"http://en.wikipedia.org/wiki/Zoo_Entertainment"};
}

	
