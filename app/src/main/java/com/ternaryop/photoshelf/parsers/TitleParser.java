package com.ternaryop.photoshelf.parsers;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ternaryop.utils.StringUtils;

public class TitleParser {
    private static final Pattern titleRE = Pattern.compile("^(.*?)\\s(at the|[-\u2013|~@]|attends|arrives|leaves|at)\\s+", Pattern.CASE_INSENSITIVE);
    // used to capture the whole name when is all uppercase
    private static final Pattern uppercaseNameRE = Pattern.compile("(^[A-Z- ]*\\b)\\s(?i)(at the|at|[-–|~@]|attends|arrives|leaves)?\\s?");

    private static TitleParser instance;
    private final TitleParserConfig config;

    private TitleParser(TitleParserConfig config) {
        this.config = config;
    }

    public TitleData parseTitle(String title) {
        TitleData titleData = new TitleData();

        title = config.applyBlackList(title);
        title = StringUtils.replaceUnicodeWithClosestAscii(title);

        title = setWho(title, titleData);

        TitleDateComponents dateComponents = new TitleDateComponents(title);
        setLocationAndCity(titleData, parseLocation(title, dateComponents));

        titleData.setWhen(dateComponents.format());
        titleData.setTags(new String[] {titleData.getWho(), titleData.getLocation()});

        return titleData;
    }

    private String setWho(String title, TitleData titleData) {
        Matcher m = uppercaseNameRE.matcher(title);
        if (m.find() && m.groupCount() > 1) {
            titleData.setWho(StringUtils.stripAccents(StringUtils.capitalize(m.group(1))));
            // remove the 'who' chunk
            title = title.substring(m.regionStart() + m.group(0).length());
        } else {
            m = titleRE.matcher(title);
            if (m.find() && m.groupCount() > 1) {
                titleData.setWho(StringUtils.stripAccents(StringUtils.capitalize(m.group(1))));
                // remove the 'who' chunk
                title = title.substring(m.regionStart() + m.group(0).length());
            }
        }
        return title;
    }

    private String parseLocation(String title, TitleDateComponents dateComponents) {
        if (dateComponents.matcher == null) {
            // no date found so use all substring as location
            return title;
        }
        return title.substring(0, dateComponents.matcher.start());
    }

    private void setLocationAndCity(TitleData titleData, String loc) {
        // city names can be multi words so allow whitespaces
        Matcher m = Pattern.compile("\\s?(.*)?\\s?\\bin\\b([a-z.\\s]*).*$", Pattern.CASE_INSENSITIVE).matcher(loc);
        if (m.find() && m.groupCount() > 1) {
            titleData.setLocation(m.group(1));
            titleData.setCity(m.group(2).trim());
        } else {
            titleData.setLocation(loc);
        }
    }

    public static TitleParser instance(TitleParserConfig config) {
        if (instance == null) {
            synchronized (TitleParser.class) {
                if (instance == null) {
                    instance = new TitleParser(config);
                }
            }
        }
        return instance;
    }
}
