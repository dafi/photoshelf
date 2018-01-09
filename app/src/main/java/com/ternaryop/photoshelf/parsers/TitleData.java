package com.ternaryop.photoshelf.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


public class TitleData {
    private List<String> who = Collections.emptyList();
    private String location;
    private String city;
    private List<String> tags = Collections.emptyList();
    private String when;

    private final TitleParserConfig config;

    public TitleData(TitleParserConfig config) {
        this.config = config;
    }

    public List<String> getWho() {
        return who;
    }

    public void setWho(List<String> who) {
        this.who = new ArrayList<>(who);
    }

    public void setWhoFromString(String string) {
        if (string == null) {
            who = Collections.emptyList();
        } else {
            who = Arrays.asList(string.trim().split("(?i)\\s*(,|&|\\band\\b)\\s*"));
        }
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        // remove all non alnum chars (except single and double quotes) from the end
        location = location.replaceAll("[^\\p{Alnum}|\"|']*$", "").trim();

        if (location.isEmpty()) {
            this.location = null;
            return;
        }
        if (hasLocationPrefix(location)) {
            // lowercase the first character
            location = location.substring(0, 1).toLowerCase(Locale.ENGLISH) + location.substring(1);
        } else {
            location = "at the " + location;
        }
        this.location = location;
    }

    private boolean hasLocationPrefix(String location) {
        for (LocationPrefix lp : config.getLocationPrefixes()) {
            if (lp.hasLocationPrefix(location)) {
                return true;
            }
        }
        return false;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String aCity) {
        if (aCity == null) {
            city = null;
        } else {
            city = expandAbbreviation(aCity.trim());
        }
    }

    private String expandAbbreviation(String aCity) {
        if (aCity.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Pattern> e : config.getCities().entrySet()) {
            if (e.getKey().equalsIgnoreCase(aCity) || e.getValue().matcher(aCity).find()) {
                return e.getKey();
            }
        }
        return aCity;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        ArrayList<String> list = new ArrayList<>();
        for (String tag1 : tags) {
            if (tag1 == null) {
                continue;
            }
            String tag = tag1
                    .replaceFirst("[0-9]*(st|nd|rd|th)?", "")
                    .replaceAll("\"|'", "")
                    .trim();
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

        formatWho(whoTagOpen, whoTagClose, descTagOpen, descTagClose, sb);
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

    public void formatWho(String whoTagOpen, String whoTagClose, String descTagOpen, String descTagClose, StringBuilder sb) {
        if (who.isEmpty()) {
            return;
        }
        boolean appendSep = false;
        for (int i = 0; i < who.size() - 1; i++) {
            if (appendSep) {
                sb.append(", ");
            } else {
                appendSep = true;
            }
            sb.append(who.get(i));
        }
        if (who.size() > 1) {
            sb.insert(0, whoTagOpen)
                    .append(whoTagClose)
                    .append(descTagOpen)
                    .append(" and ")
                    .append(descTagClose);
        }
        sb.append(whoTagOpen)
                .append(who.get(who.size() - 1))
                .append(whoTagClose)
                .append(" ");
    }
}
