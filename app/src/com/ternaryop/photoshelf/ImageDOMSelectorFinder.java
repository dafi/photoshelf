package com.ternaryop.photoshelf;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Pattern;

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
				InputStream is;
				try {
					is = context.openFileInput("domSelectors.json");
				} catch (FileNotFoundException ex) {
					is = context.getAssets().open("domSelectors.json");
				}
				BufferedReader bis = new BufferedReader(new InputStreamReader(is));
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
		if (url != null) {
			for (String domainRE : domainMap.keySet()) {
				if (Pattern.compile(domainRE).matcher(url).find()) {
					return (String) domainMap.get(domainRE);
				}
			}
		}
		return null;
	}
}
