package com.ternaryop.photoshelf.parsers;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.TextUtils;

import com.ternaryop.utils.JSONUtils;
import com.ternaryop.utils.StringUtils;
import org.json.JSONObject;

public class TitleParser {
    private static final Pattern titleRE = Pattern.compile("^(.*?)\\s(at the|[-\u2013|~@]|attends|arrives|leaves|at)\\s+", Pattern.CASE_INSENSITIVE);

    private static final String[] months = {"", "January",
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
    private static final HashMap<String, String> monthsShort = new HashMap<String, String>();
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

    private static TitleParser instance;
    private static final String TITLEPARSER_FILENAME = "titleParser.json";

    private Pattern blackListRegExpr;
    private static boolean isUpgraded;

    /**
     * Fill parseInfo with day, month, year, matched
     */
    protected Map<String, Object> parseDate(String title) {
        int day = 0;
        String monthStr = null;
        int year = 0;
        
        // handle dates in the form Jan 10, 2010 or January 10 2010 or Jan 15
        Matcher m = Pattern.compile("(-|,|on)\\s+\\(?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[^0-9]*([0-9]*)[^0-9]*([0-9]*)\\)?.*$", Pattern.CASE_INSENSITIVE).matcher(title);
        if (m.find() && m.groupCount() > 2) {
            day = m.group(3).length() != 0 ? Integer.parseInt(m.group(3)) : 0;
            monthStr = monthsShort.get(m.group(2).toLowerCase(Locale.getDefault()));
            if (m.groupCount() == 4 && m.group(4).length() > 0) {
                year = Integer.parseInt(m.group(4));
                if (year < 100) {
                    year += 2000;
                }
            }
        } else {
            // handle dates in the form dd/dd/dd?? or (dd/dd/??)
            m = Pattern.compile("\\s+\\(?([0-9]{2}).([0-9]{1,2}).([0-9]{2,4})\\)?").matcher(title);
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
                // the swap above could get an invalid date
                if (monthInt > 12) {
                    day = -1;
                    year = 0;
                } else {
                    monthStr = months[monthInt];
                }
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
        int currYear = Calendar.getInstance().get(Calendar.YEAR);
        if (year < 2000) {
            year = currYear;
        } else if (year > currYear) {
            year = currYear;
        }
        dateComponents.put("year", year + "");
        if (m != null) {
            dateComponents.put("matched", m);
        }
        
        return dateComponents;
    }

    private TitleParser(Context context) {
        InputStream is = null;
        try {
            if (blackListRegExpr != null) {
                return;
            }
            try {
                is = context.openFileInput(TITLEPARSER_FILENAME);
                // if an imported file exists and its version is minor than the file in assets we delete it
                if (!isUpgraded) {
                    isUpgraded = true;
                    JSONObject jsonPrivate = JSONUtils.jsonFromInputStream(is);
                    JSONObject jsonAssets = JSONUtils.jsonFromInputStream(context.getAssets().open(TITLEPARSER_FILENAME));
                    int privateVersion = jsonPrivate.getInt("version");
                    int assetsVersion = jsonAssets.getInt("version");
                    if (privateVersion < assetsVersion) {
                        is.close();
                        context.deleteFile(TITLEPARSER_FILENAME);
                    }
                    createBlackListRegExpr(jsonAssets);
                    return;
                }
            } catch (FileNotFoundException ex) {
                if (is != null) try { is.close(); is = null; } catch (Exception ignored) {}
                is = context.getAssets().open(TITLEPARSER_FILENAME);
            }
            createBlackListRegExpr(JSONUtils.jsonFromInputStream(is));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }

    private void createBlackListRegExpr(JSONObject jsonAssets) throws Exception {
        List<Object> blackList = new ArrayList<Object>();
        blackList.addAll(JSONUtils.toList(jsonAssets.getJSONObject("blackList").getJSONArray("regExprs")));
        blackListRegExpr = Pattern.compile("(" + TextUtils.join("|", blackList) + ")", Pattern.CASE_INSENSITIVE);
    }

    public TitleData parseTitle(String title) {
        TitleData titleData = new TitleData();

        title = blackListRegExpr.matcher(title).replaceAll("");
        title = StringUtils.replaceUnicodeWithClosestAscii(title);

        Matcher m = titleRE.matcher(title);
        if (m.find() && m.groupCount() > 1) {
          titleData.setWho(StringUtils.stripAccents(StringUtils.capitalize(m.group(1))));
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
        m = Pattern.compile("\\s?(.*)?\\s?\\bin\\b([a-z.\\s]*).*$", Pattern.CASE_INSENSITIVE).matcher(loc);
        if (m.find() && m.groupCount() > 1) {
            titleData.setLocation(m.group(1));
            titleData.setCity(m.group(2).trim());
        } else {
            titleData.setLocation(loc);
        }

        String when = "";
        if (dateComponents.get("day") != null) {
            when = dateComponents.get("day") + " ";
        }
        if (dateComponents.get("month") != null) {
            when += dateComponents.get("month") + ", ";
        }
        when += dateComponents.get("year");

        titleData.setWhen(when);
        titleData.setTags(new String[] {titleData.getWho(), titleData.getLocation()});

        return titleData;
    }

    public static TitleParser instance(Context context) {
        if (instance == null) {
            synchronized (TitleParser.class) {
                if (instance == null) {
                    instance = new TitleParser(context);
                }
            }
        }
        return instance;
    }
}
