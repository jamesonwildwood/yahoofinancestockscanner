package com.jamesontriplett.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ScreenScraper {

	private static final String SERVICE = "http://finance.yahoo.com/q/ae?s=";
	private static final String SYMBOL = "Symbol";

	private static String desiredFields[] = { "Earnings Est_No. of Analysts_3",
			"Revenue Est_No. of Analysts_3", "Earnings History_EPS Est_3",
			"Earnings History_EPS Est_4", "Earnings History_Surprise %_3",
			"Earnings History_Surprise %_4", "Growth Est_Next Qtr._1",
			"Growth Est_This Year_1", "Growth Est_Next Year_1",
			"Growth Est_Next 5 Years (per annum)_1",
			"Growth Est_PEG Ratio (avg. for comparison categories)_1" };

	public static void main(String args[]) {
		try {
			String inputfile = "input.csv";
			int i = 0;
			String arg;
			boolean vflag = false;

			while (i < args.length && args[i].startsWith("-")) {
				arg = args[i++];

				// use this type of check for arguments that require arguments
				if (arg.equals("-input")) {
					if (i < args.length)
						inputfile = args[i++];
					else
						System.err.println("-input requires a filename");
					if (vflag)
						System.out.println("input file = " + inputfile);
				} else
					System.err.println("Usage: ScreenScraper [-input afile] ");

				// use this type of check for a series of flag arguments
			}

			File f = new File(inputfile);
			if (f.exists()) {

				System.out.println("Using input file = " + inputfile);
				List<CSVRecord> myInput = getInputFromCSV(f);
				List<Map<String, String>> rawdata = getDataFromRecord(myInput);
				// Printing the output to a CVS file.
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
						"yyyyMMdd-hhmm");
				String dateAsString = simpleDateFormat.format(new Date());
				generateOutputCSV("./output-" + dateAsString + "-1-PRE.csv",
						rawdata);
				System.out.println();

				List<Map<String, String>> PostEPSFilter;
				PostEPSFilter = EPSFilter.Filter(rawdata);
				generateOutputCSV("./output-" + dateAsString + "-2-EPS.csv",
						PostEPSFilter);

				List<Map<String, String>> PostFRFilter;
				PostFRFilter = FundimentalRulesFilter.Filter(PostEPSFilter);
				generateOutputCSV("./output-" + dateAsString
						+ "-3-Finished.csv", PostFRFilter);

			} else
				System.out.println("File " + inputfile
						+ " Does not Exist!  Ending");

		} catch (IOException e) {
			System.out.println("Error occured while processing CSV file.");
			e.printStackTrace();
		}
	}

	public static List<Map<String, String>> getDataFromRecord(
			List<CSVRecord> input) {
		List<Map<String, String>> output = new ArrayList<Map<String, String>>();
		String symbol;
		Map<String, String> filledMap;
		CSVRecord PreMap;
		System.out.println("Got " + input.size() + " Symbols, Processed: ");
		Iterator<CSVRecord> iterator = input.iterator();
		while (iterator.hasNext()) {
			PreMap = iterator.next();
			symbol = PreMap.get(SYMBOL);
			//Go Scrape All the Data
			filledMap = getDataBySymbol(symbol);
			//Merge the CSVRecord into the FilledMap
			filledMap.putAll(PreMap.toMap());
			//add it to the output list
			output.add(filledMap);
			System.out.print("#");
		}
		// System.out.println(output);
		return output;
	}	
	
	private static Map<String, String> getDataBySymbol(String symbol) {

		Map<String, String> map = new HashMap<String, String>();
		map.put(SYMBOL, symbol);
		String pattern;
		try {
			Document doc = Jsoup.connect(
					(new StringBuilder(SERVICE)).append(symbol).toString())
					.get();
			Elements tables = doc.select("td table");
			for (Iterator<?> itr = tables.iterator(); itr.hasNext();) {
				Element table = (Element) itr.next();
				String as[];
				int j = (as = desiredFields).length;
				for (int i = 0; i < j; i++) {
					String keyTokens = as[i];
					String tokens[] = keyTokens.split("_");
					String value = getValue(table, tokens[0], tokens[1],
							Integer.parseInt(tokens[2]));
					if (value != null) {
						pattern = "(<font color=.*?>)*(.+?)(</font>)*";
						String updated = value.replaceAll(pattern, "$2");
						map.put(keyTokens, updated);

					}
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// System.out.println(map.toString());
		return map;
	}

	private static String getValue(Element table, String tableType,
			String field, int siblingIndex) {
		if (table.getElementsContainingOwnText(tableType).size() > 0) {
			Elements tds = table.getElementsByTag("td");
			for (Iterator<?> ittr = tds.iterator(); ittr.hasNext();) {
				Element td = (Element) ittr.next();
				if (td.html().equals(field)) {
					Element sibling = td;
					for (int i = 0; i < siblingIndex; i++)
						sibling = sibling.nextElementSibling();

					return sibling.html();
				}
			}

		}
		return null;
	}

	private static List<CSVRecord> getInputFromCSV(File csvFile)
			throws IOException {
		FileReader fr = new FileReader(csvFile);
		CSVParser csvp = new CSVParser(fr, CSVFormat.DEFAULT);
		Map<String, Integer> headers = csvp.getHeaderMap();
		
		//Get Header map and index of SYMBOL
		try{
			//Sole purpose of i is to check to see if Symbol exists
			@SuppressWarnings("unused")
			int i= headers.get(SYMBOL).intValue();
			List<CSVRecord> list = csvp.getRecords();
			csvp.close();
			return list;
		}
		catch(NullPointerException e){
			System.err.println("No " + SYMBOL + " Column Found");
			csvp.close();
			throw new IOException("Invalid File Format");
		}

	}

	private static void generateOutputCSV(String outputFilePath,
			List<Map<String, String>> output) throws IOException {
		generateOutputCSV("output", outputFilePath, output);
	}

	private static void generateOutputCSV(String directory,
			String outputFilePath, List<Map<String, String>> output)
			throws IOException {
		// Create file
		File dir = new File(directory);
		dir.mkdir();
		FileWriter fstream = new FileWriter(directory + "\\" + outputFilePath);
		BufferedWriter out = new BufferedWriter(fstream);
		int counter = 1;
		// Set keySet = null;
		for (Iterator<Map<String, String>> itr = output.iterator(); itr
				.hasNext();) {
			Map<String, String> map = itr.next();
			if (counter == 1) {
				// keySet = map.keySet();

				out.write(SYMBOL + ",");
				out.write("Composite Rating" + ",");
				for (int i = 0; i < desiredFields.length; i++) {
					out.write(desiredFields[i] + ",");
				}
				// for(Iterator<String> keyItr = keySet.iterator() ;
				// keyItr.hasNext();){
				// out.write(keyItr.next() + ",");
				// }

				out.write("\n");
			}
			out.write(map.get(SYMBOL) + ",");
			out.write(map.get("Composite Rating") + ",");
			for (int i = 0; i < desiredFields.length; i++) {
				out.write(map.get(desiredFields[i]) + ",");
			}
			// keySet = map.keySet();
			// for(Iterator<String> keyItr = keySet.iterator() ;
			// keyItr.hasNext();){
			// out.write(map.get(keyItr.next()) + ",");
			// }
			out.write("\n");

			counter++;
		}
		// Close the output stream
		out.close();
	}

}
