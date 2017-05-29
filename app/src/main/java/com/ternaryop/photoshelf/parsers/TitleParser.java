package com.ternaryop.photoshelf.parsers;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ternaryop.utils.StringUtils;

public class TitleParser {
    private static final Pattern titleRE = Pattern.compile("(.*?)\\s+(at the|at|in|on the|on|attends?|arrives?|leaves?|(night\\s+)?out|[-–|~@])\\s", Pattern.CASE_INSENSITIVE);

    private static TitleParser instance;
    private final TitleParserConfig config;

    private TitleParser(TitleParserConfig config) {
        this.config = config;
    }

    public TitleData parseTitle(String title) {
        return parseTitle(title, false);
    }

    public TitleData parseTitle(String title, boolean swapDayMonth) {
        return parseTitle(title, swapDayMonth, true);
    }

    public TitleData parseTitle(String title, boolean swapDayMonth, boolean checkDateInTheFuture) {
        TitleData titleData = new TitleData();

        title = config.applyList(config.getTitleCleanerList(), title).trim();
        title = StringUtils.replaceUnicodeWithClosestAscii(title);

        title = setWho(title, titleData);

        TitleDateComponents dateComponents = new TitleDateComponents(title, swapDayMonth, checkDateInTheFuture);
        setLocationAndCity(titleData, parseLocation(title, dateComponents));

        titleData.setWhen(dateComponents.format());
        titleData.setTags(titleData.getWho());

        return titleData;
    }

    private String setWho(String title, TitleData titleData) {
        Matcher m = titleRE.matcher(title);
        if (m.find() && m.groupCount() > 1) {
            titleData.setWhoFromString(StringUtils.stripAccents(StringUtils.capitalize(m.group(1))));
            // remove the 'who' chunk and any not alphabetic character (eg the dash used to separated "who" from location)
            if (Character.isLetter(m.group(2).charAt(0))) {
                title = title.substring(m.group(1).length());
            } else {
                title = title.substring(m.group(0).length());
            }
        }
        return title;
    }

    private String parseLocation(String title, TitleDateComponents dateComponents) {
        if (dateComponents.datePosition < 0) {
            // no date found so use all substring as location
            return title;
        }
        return title.substring(0, dateComponents.datePosition);
    }

    private void setLocationAndCity(TitleData titleData, String loc) {
        // city names can be multi words so allow whitespaces
        Matcher m = Pattern.compile("\\s?(.*)?\\s?\\bin\\b([a-z.\\s']*).*$", Pattern.CASE_INSENSITIVE).matcher(loc);
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
