package com.ternaryop.photoshelf.parsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class TitleData {
    private static final String[] locationPrefixes = {"attends", "shopping", "out and about", "arrives", "at the"};

    private static final HashMap<String, String> cities = new HashMap<String, String>();

    static {
        cities.put("LA", "Los Angeles");
        cities.put("L.A", "Los Angeles");
        cities.put("L.A.", "Los Angeles");
        cities.put("NY", "New York");
        cities.put("N.Y.", "New York");
        cities.put("NYC", "New York City");
    }

    private String who;
    private String location;
    private String city;
    private List<String> tags = Collections.emptyList();
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
        location = location.replaceAll("[^\\p{Alpha}]*$", "").trim();

        if (location.isEmpty()) {
        	this.location = null;
        	return;
        }
        boolean hasLocationPrefix = false;
        for (String prefix : locationPrefixes) {
            if (location.toLowerCase(Locale.ENGLISH).startsWith(prefix)) {
                hasLocationPrefix = true;
                break;
            }
        }
        if (hasLocationPrefix) {
            // lowercase the first character
            location = location.substring(0, 1).toLowerCase(Locale.ENGLISH) + location.substring(1);
        } else {
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        ArrayList<String> list = new ArrayList<String>();
        for (String tag1 : tags) {
            if (tag1 == null) {
                continue;
            }
            String tag = tag1
                    .replace("[0-9]*(st|nd|rd|th)?", "")
                    .replaceAll("\"|'", "");
            for (String prefix : locationPrefixes) {
                tag = tag.replaceFirst(prefix, "");
            }
            tag = tag.trim();
            if (tag.length() > 0) {
                list.add(tag);
            }

        }
        this.tags = list;
    }

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when.trim();
    }

    public String toHtml() {
        return format("<strong>", "</strong>", "<em>", "</em>");
    }

    public String format(String whoTagOpen, String whoTagClose, String descTagOpen, String descTagClose) {
        StringBuilder sb = new StringBuilder();

        if (who != null) {
            sb.append(whoTagOpen)
                    .append(who)
                    .append(whoTagClose)
                    .append(" ");
        }
        if (location != null || when != null || city != null) {
            sb.append(descTagOpen);
            if (location != null) {
                sb.append(location);
                if (city == null) {
                    sb.append(" ");
                } else {
                    sb.append(", ");
                }
            }
            if (city != null) {
                sb
                .append(city)
                .append(" ");
            }
            if (when != null) {
                sb
                .append("(")
                .append(when)
                .append(")");
            }
            sb.append(descTagClose);
        }
        return sb.toString();
    }
}
