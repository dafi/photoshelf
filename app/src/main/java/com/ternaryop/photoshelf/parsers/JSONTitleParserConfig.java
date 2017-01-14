package com.ternaryop.photoshelf.parsers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by dave on 09/01/16.
 * Read from json file
 */
public class JSONTitleParserConfig implements TitleParserConfig {
    private List<TitleParserRegExp> titleCleanerList;

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
    }

    @Override
    public List<TitleParserRegExp> getTitleCleanerList() {
        return titleCleanerList;
    }

    @Override
    public String applyList(List<TitleParserRegExp> titleParserRegExpList, String input) {
        return TitleParserRegExp.applyList(titleParserRegExpList, input);
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
