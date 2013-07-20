package com.ternaryop.phototumblrshare.parsers;


public class TitleData {
    private static String format = "<p><strong>$who</strong> <em>at the $where_loc, $where_city ($when)</em></p>";

    public String who = "***";
    public String location = "***";
    public String city = "***";
    public String tags = "***";
    public String when;

    public String toString() {
    	return format
    			.replace("$who", who)
    			.replace("$where_loc", location)
    			.replace("$where_city", city)
    			.replace("$when", when);
    }
}
