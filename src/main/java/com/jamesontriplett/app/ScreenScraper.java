package com.jamesontriplett.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
	protected static final String SYMBOL = "Symbol";

	protected static String desiredFields[] = { "Earnings Est_No. of Analysts_3",
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
				processFile(f);
			} else
				System.out.println("File " + inputfile
						+ " Does not Exist!  Ending");

		} catch (IOException e) {
			System.err.println("Error occured while processing CSV file.");
			e.printStackTrace();
		}
	}

	private static void processFile(File f) throws IOException {

		System.out.println("Using input file = " + f.getName());
		List<CSVRecord> myInput = getInputFromCSV(f);
		List<Map<String, String>> rawdata = getDataFromRecord(myInput);
		// Printing the output to a CSV file.
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"yyyyMMdd-hhmm");
		String dateAsString = simpleDateFormat.format(new Date());
		generateOutputCSV("./output-" + dateAsString + "-1-PRE.csv", rawdata);
		System.out.println();

		List<Map<String, String>> PostEPSFilter;
		PostEPSFilter = EPSFilter.Filter(rawdata);
		generateOutputCSV("./output-" + dateAsString + "-2-EPS.csv",
				PostEPSFilter);

		List<Map<String, String>> PostFRFilter;
		PostFRFilter = FundimentalRulesFilter.Filter(PostEPSFilter);
		generateOutputCSV("./output-" + dateAsString + "-3-Finished.csv",
				PostFRFilter);

	}

	
	
	/**
	 * Takes a list of CSVRecords and executes a scrape on all the records.
	 * produces a List of Maps of all the new data
	 * @param input A list of CSVRecords to process
	 * @return
	 */
	public static List<Map<String, String>> getDataFromRecord(
			List<CSVRecord> input) {
		List<Map<String, String>> output = new ArrayList<Map<String, String>>();
		String symbol;
		Map<String, String> filledMap;
		CSVRecord PreMap;
		System.out.println("Got " + input.size() + " Symbols, Processed: ");
		Iterator<CSVRecord> iterator = input.iterator();
		YahooScreenScraper yss = new YahooScreenScraper(SYMBOL,desiredFields );

		while (iterator.hasNext()) {
			PreMap = iterator.next();
			symbol = PreMap.get(SYMBOL);
			// Go Scrape All the Data
			
			filledMap = yss.getDataBySymbol(symbol);
			// Merge the CSVRecord into the FilledMap
			filledMap.putAll(PreMap.toMap());
			// add it to the output list
			output.add(filledMap);
			System.out.print("#");
		}
		// System.out.println(output);
		return output;
	}


	/**
	 * Takes a File and prints it to console. For Testing purposes
	 * 
	 * @param csvFile
	 * @throws Exception
	 */
	private static void printFile(File csvFile) throws Exception {
		FileInputStream input = new FileInputStream(csvFile);
		FileChannel channel = input.getChannel();
		byte[] buffer = new byte[256 * 1024];
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

		try {
			for (int length = 0; (length = channel.read(byteBuffer)) != -1;) {
				System.out.write(buffer, 0, length);
				byteBuffer.clear();
			}
		} finally {
			input.close();
		}
	}

	/**
	 * Takes a CSV File and parses into a List of CSVRecords objects. Also verifies
	 * that the SYMBOL variable exists in the csv since that is the critical piece
	 * @param csvFile the file to input
	 * @return List of CSVRecords
	 * @throws IOException
	 */
	private static List<CSVRecord> getInputFromCSV(File csvFile)
			throws IOException {
		FileReader fr = new FileReader(csvFile);
		// For Testing
		try {
			printFile(csvFile);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		CSVParser csvp = new CSVParser(fr, CSVFormat.EXCEL.withHeader());
		List<CSVRecord> list = csvp.getRecords();
		Map<String, Integer> headers = csvp.getHeaderMap();

		// Get Header map and index of SYMBOL
		try {
			// Sole purpose of i is to check to see if Symbol exists
			@SuppressWarnings("unused")
			int i = headers.get(SYMBOL).intValue();
			csvp.close();
			return list;
		} catch (NullPointerException e) {
			System.err.println("No " + SYMBOL + " Column Found");
			csvp.close();
			throw new IOException("Invalid File Format");
		}

	}

	private static void generateOutputCSV(String outputFilePath,
			List<Map<String, String>> output) throws IOException {
		generateOutputCSV("output", outputFilePath, output);
	}

	/**
	 * Takes a List of Maps and outputs to a CSV File
	 * @param directory the directory to output to
	 * @param outputFilePath name of the output file
	 * @param output List of Maps to output to the csv file
	 * @throws IOException
	 */
	private static void generateOutputCSV(String directory,
			String outputFilePath, List<Map<String, String>> output)
			throws IOException {
		// Create file
		File dir = new File(directory);
		dir.mkdir();
		FileWriter fstream = new FileWriter(directory + "/" + outputFilePath);
		BufferedWriter out = new BufferedWriter(fstream);
		int counter = 1;
		Set<String> keySet = null;
		for (Iterator<Map<String, String>> itr = output.iterator(); itr
				.hasNext();) {
			Map<String, String> map = itr.next();
			if (counter == 1) {
				keySet = map.keySet();

				out.write(SYMBOL);
				// for (int i = 0; i < desiredFields.length; i++) {
				// out.write(desiredFields[i] + ",");
				// }
				for (Iterator<String> keyItr = keySet.iterator(); keyItr
						.hasNext();) {
					out.write("," + keyItr.next());
				}

				out.write("\n");
			}
			out.write(map.get(SYMBOL));
			keySet = map.keySet();
			for (Iterator<String> keyItr = keySet.iterator(); keyItr.hasNext();) {
				out.write("," + map.get(keyItr.next()) );
			}
			out.write("\n");

			counter++;
		}
		// Close the output stream
		out.close();
	}

}
