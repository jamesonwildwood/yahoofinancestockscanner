package com.jamesontriplett.app;

//import org.apache.commons.logging.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FundimentalRulesFilter {
	// private static Log logger = LogFactory.getLog(ScrapeOutputFilter.class);

	private static String desiredFields[] = { "Earnings Est_No. of Analysts_3",
			"Revenue Est_No. of Analysts_3", "Earnings History_EPS Est_3",
			"Earnings History_EPS Est_4", "Earnings History_Surprise %_3",
			"Earnings History_Surprise %_4", "Growth Est_Next Qtr._1",
			"Growth Est_This Year_1", "Growth Est_Next Year_1",
			"Growth Est_Next 5 Years (per annum)_1",
			"Growth Est_PEG Ratio (avg. for comparison categories)_1" };

	public static List<Map<String, String>> Filter(
			List<Map<String, String>> input) throws IOException {

		List<Map<String, String>> filteredOutput = new ArrayList<Map<String, String>>();

		System.out.println("-------Begin Fundimentals Filters -------");

		for (Iterator<Map<String, String>> itr = input.iterator(); itr
				.hasNext();) {
			Map<String, String> map = itr.next();
			// Take each line and split into components
			try {

				double analysts = Double.parseDouble(map.get(desiredFields[0]));
				double surprise1 = Double.parseDouble((map
						.get(desiredFields[4]).trim()).replace("%", ""));
				double surprise2 = Double.parseDouble((map
						.get(desiredFields[5]).trim()).replace("%", ""));
//				double eps1 = Double.parseDouble(map.get(desiredFields[2]));
//				double eps2 = Double.parseDouble(map.get(desiredFields[3]));
//				double peg = Double.parseDouble(map.get(desiredFields[10]));

				double ThisYearGrowth = Double.parseDouble((map
						.get(desiredFields[7]).trim()).replace("%", ""));
				double NextYearGrowth = Double.parseDouble((map
						.get(desiredFields[8]).trim()).replace("%", ""));
				double Next5YearGrowth = Double.parseDouble((map
						.get(desiredFields[9]).trim()).replace("%", ""));
				// if eps this year is less than eps next year
				if (surprise1 >= 0 && surprise2 >= 0) {
					if (ThisYearGrowth > 0) {
						if (NextYearGrowth > 15) {
							if (Next5YearGrowth > 15) {
								filteredOutput.add(map);
							} else
								System.out.println("REJECTED "+map.get("Symbol")+"; next 5 growth: "
										+ Next5YearGrowth );
						} else
							System.out.println("REJECTED "+map.get("Symbol")+"; next year growth: "
									+ NextYearGrowth );
					} else
						System.out.println("REJECTED "+map.get("Symbol")+"; this year growth: "
								+ ThisYearGrowth);
				} else
					System.out.println("REJECTED "+map.get("Symbol")+"; Surprise 1/2 or Analysts: "
							+ surprise1 + "/" + surprise2 + "; Analysts:" + analysts
							);
			} catch (NumberFormatException e) {
				System.out.println("Rejected: "+map.get("Symbol")+"; ");
				System.out.println("Bad Number format" + e.getMessage());
				// do nothing since this will be an invalid line.
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("Rejected: "+map.get("Symbol")+"; ");
				System.out.println("Bad Array: " + e.getMessage());

			}

		}
		return filteredOutput;
	}
}
