package com.ternaryop.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    private static char[] unicodeChars = {
        '\u00AB', '\u00AD', '\u00B4', '\u00BB', '\u00F7', '\u01C0', '\u01C3', '\u02B9',
        '\u02BA', '\u02BC', '\u02C4', '\u02C6', '\u02C8', '\u02CB', '\u02CD', '\u02DC',
        '\u0300', '\u0301', '\u0302', '\u0303', '\u030B', '\u030E', '\u0331', '\u0332',
        '\u0338', '\u0589', '\u05C0', '\u05C3', '\u066A', '\u066D', '\u200B', '\u2010',
        '\u2011', '\u2012', '\u2013', '\u2014', '\u2015', '\u2016', '\u2017', '\u2018',
        '\u2019', '\u201A', '\u201B', '\u201C', '\u201D', '\u201E', '\u201F', '\u2032',
        '\u2033', '\u2034', '\u2035', '\u2036', '\u2037', '\u2038', '\u2039', '\u203A',
        '\u203D', '\u2044', '\u204E', '\u2052', '\u2053', '\u2060', '\u20E5', '\u2212',
        '\u2215', '\u2216', '\u2217', '\u2223', '\u2236', '\u223C', '\u2264', '\u2265',
        '\u2266', '\u2267', '\u2303', '\u2329', '\u232A', '\u266F', '\u2731', '\u2758',
        '\u2762', '\u27E6', '\u27E8', '\u27E9', '\u2983', '\u2984', '\u3003', '\u3008',
        '\u3009', '\u301B', '\u301C', '\u301D', '\u301E', '\uFEFF'
    };
    
    private static String[] closestToUnicode = {
        "\"","-","'","\"","/","|","!","'",
        "\"","'","^","^","'","`","_","~",
        "`","'","^","~","\"","\"","_","_",
        "/",":","|",":","%","*","","-",
        "-","-","-","-","--","||","_","'",
        "'",",","'","\"","\"","\"","\"","'",
        "\"","'''","`","\"","'''","^","<",">",
        "?","/","*","%","~","","\\","-",
        "/","\\","*","|",":","~","<=",">=",
        "<=",">=","^","<",">","#","*","|",
        "!","[","<",">","{","}","\"","<",
        ">","]","~","\"","\"","",
        };
    
	/**
	 * Surround all pattern strings found on text with a <b> (bold) tag
	 * @param pattern
	 * @param text
	 * @return
	 */
	public static String htmlHighlightPattern(String pattern, String text) {
		StringBuffer sb = new StringBuffer();
		Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
	
		while (m.find()) {
			// get substring to preserve case
			m.appendReplacement(sb, "<b>" + text.substring(m.start(), m.end()) + "</b>");
		}
		m.appendTail(sb);
	
		return sb.toString();
	}

    public static String capitalize(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        boolean upcase = true;
        
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);

            if (ch == '_' || ch == '-' || Character.isWhitespace(ch)) {
                upcase = true;
            } else {
                if (upcase) {
                    ch = Character.toUpperCase(ch);
                    upcase = false;
                } else {
                	ch = Character.toLowerCase(ch);
                }
            }
            sb.append(ch);
        }

        return sb.toString();
    }

    /**
     * Strip all specified HTML tags contained into string
     * @param tags tags separated by pipe (eg "a|br|img")
     * @param string
     * @return stripped string
     */
    public static String stripHtmlTags(String tags, String string) {
        return Pattern.compile("<\\/?(" + tags + ").*?>", Pattern.CASE_INSENSITIVE)
                .matcher(string).replaceAll("");
    }

    public static String replaceUnicodeWithClosestAscii(String str) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            int pos = Arrays.binarySearch(unicodeChars, ch);
            if (pos >= 0) {
                sb.append(closestToUnicode[pos]);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    
    // http://commons.apache.org/proper/commons-lang/apidocs/src-html/org/apache/commons/lang3/StringUtils.html#line.761
    public static String stripAccents(final String input) {
        if (input == null) {
            return null;
        }
        final Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");//$NON-NLS-1$
        final String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Note that this doesn't correctly remove ligatures...
        return pattern.matcher(decomposed).replaceAll("");//$NON-NLS-1$
    }    
}
