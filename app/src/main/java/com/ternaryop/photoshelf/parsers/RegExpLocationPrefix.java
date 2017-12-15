package com.ternaryop.photoshelf.parsers;

import java.util.regex.Pattern;

/**
 * Created by dave on 04/12/17.
 * Use regular expression to match location prefix
 */

public class RegExpLocationPrefix implements LocationPrefix {
    private final Pattern pattern;

    public RegExpLocationPrefix(String regExp) {
        pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean hasLocationPrefix(String location) {
        return pattern.matcher(location).find();
    }

    @Override
    public String removePrefix(String target) {
        return pattern.matcher(target).replaceFirst("");
    }
}
