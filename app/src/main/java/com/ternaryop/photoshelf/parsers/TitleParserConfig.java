package com.ternaryop.photoshelf.parsers;

import java.util.regex.Pattern;

/**
 * Created by dave on 21/03/15.
 * Access to TitleParser configuration
 */
public interface TitleParserConfig {
    Pattern getBlackListRegExpr();
}
