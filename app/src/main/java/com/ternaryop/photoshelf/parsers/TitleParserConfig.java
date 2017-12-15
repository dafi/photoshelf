package com.ternaryop.photoshelf.parsers;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by dave on 21/03/15.
 * Access to TitleParser configuration
 */
public interface TitleParserConfig {
    List<TitleParserRegExp> getTitleCleanerList();
    String applyList(List<TitleParserRegExp> titleParserRegExpList, String input);
    Pattern getTitleParserPattern();
    List<LocationPrefix> getLocationPrefixes();
    Map<String, Pattern> getCities();

    class TitleParserRegExp {
        final String pattern;
        final String replacer;

        public TitleParserRegExp(final String pattern, final String replacer) {
            this.pattern = pattern;
            this.replacer = replacer;
        }

        public static String applyList(final List<TitleParserRegExp> titleParserRegExpList, final String input) {
            String result = input;

            for (TitleParserRegExp re : titleParserRegExpList) {
                result = result.replaceAll(re.pattern, re.replacer);
            }
            return result;
        }
    }
}
