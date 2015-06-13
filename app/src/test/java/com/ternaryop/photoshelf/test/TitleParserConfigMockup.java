package com.ternaryop.photoshelf.test;

import java.util.Collections;
import java.util.List;

import com.ternaryop.photoshelf.parsers.TitleParserConfig;

/**
 * Created by dave on 21/03/15.
 * Used by tests
 */
public class TitleParserConfigMockup implements TitleParserConfig {
    public TitleParserConfigMockup(String configPath) throws Exception {
    }

    @Override
    public List<TitleParserRegExp> getBlackListRegExpr() {
        return Collections.emptyList();
    }

    @Override
    public String applyBlackList(String input) {
        return input;
    }
}
