package com.ternaryop.utils;

import java.net.URL;
import java.net.URLConnection;

public class URLUtils {
	/* Resolve a possible shorten url to the original one */
	public static String resolveShortenURL(String strURL) {
		URLConnection conn = null;
		try {
			URL inputURL = new URL(strURL);
			conn = inputURL.openConnection();
			conn.getHeaderFields();
	        return conn.getURL().toString();
		} catch (Exception e) {
			System.out.println(e);
			return strURL;
		}
	}
}
