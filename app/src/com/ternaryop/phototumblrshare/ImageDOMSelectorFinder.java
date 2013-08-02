package com.ternaryop.phototumblrshare;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.json.JSONObject;

import android.content.Context;

import com.ternaryop.utils.JSONUtils;

/**
 * Obtain the DOM selector used to search the image contained inside a given url
 * 
 * @author dave
 * 
 */
public class ImageDOMSelectorFinder {
	private static HashMap<String, Object> domainMap = new HashMap<String, Object>();

	public ImageDOMSelectorFinder(Context context) {
		try {
			synchronized (domainMap) {
				BufferedReader bis = new BufferedReader(new InputStreamReader(context.getAssets().open("domSelectors.json")));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = bis.readLine()) != null) {
					builder.append(line);
				}
				if (domainMap.isEmpty()) {
					domainMap.putAll(JSONUtils.toMap(new JSONObject(builder.toString())));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getSelectorFromUrl(String url) {
		for (String domain : domainMap.keySet()) {
			if (url.startsWith(domain)) {
				return (String) domainMap.get(domain);
			}
		}
		return null;
	}
}
