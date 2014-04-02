package com.jamesontriplett.app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class YahooScreenScraper {
	
	
	public YahooScreenScraper(String sYMBOL, String[] desiredFields) {
		super();
		SYMBOL = sYMBOL;
		this.desiredFields = desiredFields;
	}

	private static final String SERVICE = "http://finance.yahoo.com/q/ae?s=";

	private String SYMBOL;

	private String desiredFields[];
	
	public Map<String, String> getDataBySymbol(String symbol) {

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

	
}
