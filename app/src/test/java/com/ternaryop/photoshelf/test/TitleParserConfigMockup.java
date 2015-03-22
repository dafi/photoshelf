package com.ternaryop.photoshelf.test;

import java.util.regex.Pattern;

import com.ternaryop.photoshelf.parsers.TitleParserConfig;

/**
 * Created by dave on 21/03/15.
 * Used by tests
 */
public class TitleParserConfigMockup implements TitleParserConfig {
    private Pattern blackListRegExpr;

    public TitleParserConfigMockup(String configPath) throws Exception {
        blackListRegExpr = Pattern.compile("(fake)", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public Pattern getBlackListRegExpr() {
        return blackListRegExpr;
    }
}
