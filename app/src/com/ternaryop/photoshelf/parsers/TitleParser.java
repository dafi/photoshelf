package com.ternaryop.photoshelf.parsers;


import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ternaryop.utils.StringUtils;

public class TitleParser {
    private static Pattern titleRE = Pattern.compile("^(.*?)\\s(at the|[-\u2013|~@]|attends|arrives|leaves)", Pattern.CASE_INSENSITIVE);

    private static String[] months = {"", "January",
                  "February",
                  "March",
                  "April",
                  "May",
                  "June",
                  "July",
                  "August",
                  "September",
                  "October",
                  "November",
                  "December"};
    private static HashMap<String, String> monthsShort = new HashMap<String, String>();
    static {
        monthsShort.put("jan", "January");
        monthsShort.put("feb", "February");
        monthsShort.put("mar", "March");
        monthsShort.put("apr", "April");
        monthsShort.put("may", "May");
        monthsShort.put("jun", "June");
        monthsShort.put("jul", "July");
        monthsShort.put("aug", "August");
        monthsShort.put("sep", "September");
        monthsShort.put("oct", "October");
        monthsShort.put("nov", "November");
        monthsShort.put("dec", "December");
    }

	private static TitleParser instance = new TitleParser();
    
    /**
     * Fill parseInfo with day, month, year, matched
     */
    protected Map<String, Object> parseDate(String title) {
    	int day = 0;
    	String monthStr = null;
    	int year = 0;
    	
        // handle dates in the form Jan 10, 2010 or January 10 2010 or Jan 15
        Matcher m = Pattern.compile("[-,]\\s+\\(?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[^0-9]*([0-9]*)[^0-9]*([0-9]*)\\)?.*$", Pattern.CASE_INSENSITIVE).matcher(title);
        if (m.find() && m.groupCount() > 1) {
            day = m.group(2).length() != 0 ? Integer.parseInt(m.group(2)) : 0;
            monthStr = monthsShort.get(m.group(1).toLowerCase(Locale.getDefault()));
            if (m.groupCount() == 3 && m.group(3).length() > 0) {
                year = Integer.parseInt(m.group(3));
            }
        } else {
            // handle dates in the form dd/dd/dd?? or (dd/dd/??)
            m = Pattern.compile("\\(?([0-9]{2}).([0-9]{1,2}).([0-9]{2,4})\\)?").matcher(title);
            if (m.find() && m.groupCount() > 1) {
                day = Integer.parseInt(m.group(1));
                int monthInt = Integer.parseInt(m.group(2));
                year = Integer.parseInt(m.group(3));
                // we have a two-digits year
                if (year < 100) {
                	year += 2000;
                }
                if (monthInt > 12) {
                    int tmp = monthInt;
                    monthInt = day;
                    day = tmp;
                }
                monthStr = months[monthInt];
            } else {
            	m = null;
            }
        }
        HashMap<String, Object> dateComponents = new HashMap<String, Object>();
        // day could be not present for example "New York City, January 11"
        if (day > 0) {
        	dateComponents.put("day",  day + "");
        }
        if (monthStr != null) {
        	dateComponents.put("month", monthStr);
        }
        if (year < 2000) {
            year = Calendar.getInstance().get(Calendar.YEAR);
        }
        dateComponents.put("year", year + "");
        if (m != null) {
        	dateComponents.put("matched", m);
        }
        
        return dateComponents;
    }

    private TitleParser() {
    	
    }

    public TitleData parseTitle(String title) {
        TitleData titleData = new TitleData();

        title = StringUtils.replaceUnicodeWithClosestAscii(title);

        Matcher m = titleRE.matcher(title);
        if (m.find() && m.groupCount() > 1) {
          titleData.setWho(StringUtils.capitalize(m.group(1)));
          // remove the 'who' chunk
          title = title.substring(m.regionStart() + m.group(0).length());
        }
        Map<String, Object> dateComponents = parseDate(title);
        Matcher dateMatcher = (Matcher) dateComponents.get("matched");
        String loc;
        if (dateMatcher == null) {
        	// no date found so use all substring as location
        	loc = title;
        } else {
    		loc = title.substring(0, dateMatcher.start());
        }
        // city names can be multi words so allow whitespaces
        m = Pattern.compile("\\s*(.*)\\s+in\\s+([a-z.\\s]*).*$", Pattern.CASE_INSENSITIVE).matcher(loc);
        if (m.find() && m.groupCount() > 1) {
            titleData.setLocation(m.group(1));
            titleData.setCity(m.group(2).trim());
        } else {
            titleData.setLocation(loc);
        }

        String when = "";
        if (dateComponents.get("day") != null) {
            when = dateComponents.get("day") + " ";
        };
        if (dateComponents.get("month") != null) {
        	when += dateComponents.get("month") + ", ";
        }
        when += dateComponents.get("year");

        titleData.setWhen(when);
        titleData.setTags(new String[] {titleData.getWho(), titleData.getLocation()});

        return titleData;
    }

    public static TitleParser instance() {
    	return instance;
    }
}
