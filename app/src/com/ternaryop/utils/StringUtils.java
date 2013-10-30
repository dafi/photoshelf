package com.ternaryop.utils;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	public static final String EMPTY = "";

	/**
	 * Adapter from org.apache.commons.lang.StringUtils.join
	 * @param iterator
	 * @param separator
	 * @return
	 */
	public static String join(Iterator<?> iterator, String separator) {
		// handle null, zero and one elements before building a buffer
		if (iterator == null) {
			return null;
		}
		if (!iterator.hasNext()) {
			return EMPTY;
		}
		Object first = iterator.next();
		if (!iterator.hasNext()) {
			return first.toString();
		}
		// two or more elements
		StringBuffer buf = new StringBuffer(256); // Java default is 16, probably too small
		if (first != null) {
			buf.append(first);
		}
		while (iterator.hasNext()) {
			if (separator != null) {
				buf.append(separator);
			}
			Object obj = iterator.next();
			if (obj != null) {
				buf.append(obj);
			}
		}
		return buf.toString();
	}

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
