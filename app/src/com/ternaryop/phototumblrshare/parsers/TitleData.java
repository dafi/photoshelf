package com.ternaryop.phototumblrshare.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.jsoup.helper.StringUtil;


public class TitleData {
	private static String format = "<p><strong>$who</strong> <em>$where_loc, $where_city ($when)</em></p>";
    private static String[] locationPrefixes = {"attends", "shopping", "out and about", "arrives"};

    private static HashMap<String, String> cities = new HashMap<String, String>();

    static {
        cities.put("LA", "Los Angeles");
        cities.put("L.A", "Los Angeles");
        cities.put("L.A.", "Los Angeles");
        cities.put("NY", "New York");
        cities.put("N.Y.", "New York");
        cities.put("NYC", "New York City");
    }

    private String who = "***";
    private String location = "***";
    private String city = "***";
    private String tags = "***";
    private String when;

    public String getWho() {
		return who;
	}

	public void setWho(String who) {
		this.who = who.trim();
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		// remove all non alpha chars from the end
        location = location.replaceAll("[^a-z]*$", "").trim();

        boolean hasLocationPrefix = false;
    	for (int i = 0; i < locationPrefixes.length; i++) {
    		if (location.toLowerCase(Locale.ENGLISH).startsWith(locationPrefixes[i])) {
    			hasLocationPrefix = true;
    			break;
    		}
    	}
    	// lowercase the first character
    	location = location.substring(0, 1).toLowerCase(Locale.ENGLISH) + location.substring(1);
    	if (!hasLocationPrefix) {
    		location = "at the " + location; 
    	}
		this.location = location;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		String decodedCity = cities.get(city.toUpperCase(Locale.getDefault()));
		if (decodedCity == null) {
			this.city = city.trim();
		} else {
			this.city = decodedCity;
		}
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < tags.length; i++) {
			String tag = tags[i]
	        		.replace("[0-9]*(st|nd|rd|th)?", "")
	        		.replaceAll("\"|'", "");
	    	for (int l = 0; l < locationPrefixes.length; l++) {
	    		tag = tag.replaceFirst(locationPrefixes[l], "");
	    	}
	    	tag = tag.trim();
	    	if (tag.length() > 0) {
	    		list.add(tag);
	    	}
			
		}
		this.tags = StringUtil.join(list, ", ");
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when.trim();
	}

    public String toString() {
    	boolean hasLocationPrefix = false;
    	for (int i = 0; i < locationPrefixes.length; i++) {
    		if (location.toLowerCase(Locale.ENGLISH).startsWith(locationPrefixes[i])) {
    			hasLocationPrefix = true;
    			break;
    		}
    	}
    	// lowercase the first character
    	String locationText = location.substring(0, 1).toLowerCase(Locale.ENGLISH) + location.substring(1);
    	if (!hasLocationPrefix) {
    		locationText = "at the " + locationText; 
    	}

    	return format
    			.replace("$who", who)
    			.replace("$where_loc", locationText)
    			.replace("$where_city", city)
    			.replace("$when", when);
    }
}
