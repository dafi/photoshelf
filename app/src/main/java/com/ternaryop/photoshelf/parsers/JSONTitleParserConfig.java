package com.ternaryop.photoshelf.parsers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 09/01/16.
 * Read from json file
 */
public class JSONTitleParserConfig implements TitleParserConfig {
    private List<TitleParserRegExp> titleCleanerList;
    private Pattern titleParserPattern;
    private List<LocationPrefix> locationPrefixes;
    private HashMap<String, Pattern> cities = new HashMap<>();

    public JSONTitleParserConfig() {
    }

    public JSONTitleParserConfig(String jsonPath) throws Exception {
        readConfig(jsonPath);
    }

    public void readConfig(String jsonPath) throws Exception {
        try (InputStream is = new FileInputStream(jsonPath)) {
            readConfig(is);
        }
    }

    public void readConfig(InputStream is) throws Exception {
        readConfig(JSONUtils.jsonFromInputStream(is));
    }

    public void readConfig(JSONObject jsonObject) throws Exception {
        titleCleanerList = createList(jsonObject, "titleCleaner", "regExprs");
        titleParserPattern = Pattern.compile(jsonObject.getString("titleParserRegExp"), Pattern.CASE_INSENSITIVE);
        readLocationPrefixes(jsonObject);
        readCities(jsonObject);
    }

    private void readCities(JSONObject jsonObject) throws JSONException {
        cities = new HashMap<>();
        final JSONObject jsonCities = jsonObject.getJSONObject("cities");
        final Iterator<String> keys = jsonCities.keys();

        while (keys.hasNext()) {
            final String k = keys.next();
            cities.put(k,  Pattern.compile((String) jsonCities.get(k)));
        }
    }

    private void readLocationPrefixes(JSONObject jsonObject) throws JSONException {
        final JSONArray jsonArray = jsonObject.getJSONArray("locationPrefixes");
        locationPrefixes = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            locationPrefixes.add(new RegExpLocationPrefix(jsonArray.getString(i)));
        }
    }

    @Override
    public List<TitleParserRegExp> getTitleCleanerList() {
        return titleCleanerList;
    }

    @Override
    public String applyList(List<TitleParserRegExp> titleParserRegExpList, String input) {
        return TitleParserRegExp.applyList(titleParserRegExpList, input);
    }

    @Override
    public Pattern getTitleParserPattern() {
        return titleParserPattern;
    }

    @Override
    public List<LocationPrefix> getLocationPrefixes() {
        return locationPrefixes;
    }

    @Override
    public Map<String, Pattern> getCities() {
        return cities;
    }

    public static ArrayList<TitleParserRegExp> createList(JSONObject jsonAssets, String rootName, String replacers) throws Exception {
        ArrayList<TitleParserRegExp> list = new ArrayList<>();
        JSONArray array = jsonAssets.getJSONObject(rootName).getJSONArray(replacers);
        for (int i = 0; i < array.length(); i++) {
            JSONArray reArray = array.getJSONArray(i);
            list.add(new TitleParserRegExp(reArray.getString(0), reArray.getString(1)));
        }
        return list;
    }
}
