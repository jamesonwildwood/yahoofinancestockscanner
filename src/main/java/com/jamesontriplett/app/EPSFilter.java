package com.jamesontriplett.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class EPSFilter {
	// private static Log logger = LogFactory.getLog(ScrapeOutputFilter.class);

	public static List<Map<String, String>> Filter(
			List<Map<String, String>> input) throws IOException {
		String desiredFields[] = { "Earnings History_EPS Est_3",
				"Earnings History_EPS Est_4",
				"Growth Est_PEG Ratio (avg. for comparison categories)_1" };
		List<Map<String, String>> filteredOutput = new ArrayList<Map<String, String>>();

		System.out.println("-------Begin EPS Filters -------");
		double eps1, eps2, peg;
		for (Iterator<Map<String, String>> itr = input.iterator(); itr
				.hasNext();) {
			{
				Map<String, String> map = itr.next();
				// Take each line and split into components
				try {
					eps1 = Double.parseDouble(map.get(desiredFields[0]));
					eps2 = Double.parseDouble(map.get(desiredFields[1]));
					peg = Double.parseDouble(map.get(desiredFields[2]));

					// if eps this year is less than eps next year
					if (eps1 <= eps2) {
						if (peg <= 2.0) {

							filteredOutput.add(map);
						} else {

							System.out.println("REJECTED: " + map.get("Symbol")
									+ "; PEG too High: " + peg );

						}
					} else {
						// logger.info("REJECTED eps growth negative: "+ eps1 +
						// "/" +eps2 + " Was: " +line );
						System.out.println("REJECTED: " + map.get("Symbol")
								+ "; eps growth negative: " + eps1 + "/" + eps2
								);

					}
				} catch (NumberFormatException e) {
					// do nothing since this will be an invalid line.
					// logger.warn("REJECTED bad Numbers " + e.getMessage() +
					// " Was: " +line );
					System.out.println("REJECTED " + map.get("Symbol")
							+ "; bad Numbers " + e.getMessage() );

				} catch (NullPointerException e){
				// do nothing since this will be an invalid line.
				// logger.warn("REJECTED bad Numbers " + e.getMessage() +
				// " Was: " +line );
				System.out.println("REJECTED " + map.get("Symbol")
						+ "; bad Numbers " + e.getMessage() );
				}
			}
		}

		return filteredOutput;
	}
}
