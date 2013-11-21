package com.ternaryop.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
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
}
