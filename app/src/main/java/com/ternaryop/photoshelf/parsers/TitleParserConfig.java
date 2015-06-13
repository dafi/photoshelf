package com.ternaryop.photoshelf.parsers;

import java.util.List;

/**
 * Created by dave on 21/03/15.
 * Access to TitleParser configuration
 */
public interface TitleParserConfig {
    public List<TitleParserRegExp> getBlackListRegExpr();
    public String applyBlackList(String input);

    public static class TitleParserRegExp {
        final String pattern;
        final String replacer;

        public TitleParserRegExp(String pattern, String replacer) {
            this.pattern = pattern;
            this.replacer = replacer;
        }
    }
}
