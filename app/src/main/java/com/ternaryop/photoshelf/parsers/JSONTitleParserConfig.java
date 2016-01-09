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
    private List<TitleParserRegExp> blackList;

    public JSONTitleParserConfig(String jsonPath) throws Exception {
        try (InputStream is = new FileInputStream(jsonPath)) {
            createBlackListRegExpr(JSONUtils.jsonFromInputStream(is));
        }
    }

    protected void createBlackListRegExpr(JSONObject jsonAssets) throws Exception {
        blackList = new ArrayList<>();
        JSONArray array = jsonAssets.getJSONObject("blackList").getJSONArray("regExprs");
        for (int i = 0; i < array.length(); i++) {
            JSONArray reArray = array.getJSONArray(i);
            blackList.add(new TitleParserRegExp(reArray.getString(0), reArray.getString(1)));
        }
    }

    @Override
    public List<TitleParserRegExp> getBlackListRegExpr() {
        return blackList;
    }

    @Override
    public String applyBlackList(String input) {
        String result = input;

        for (TitleParserRegExp re : blackList) {
            result = result.replaceAll(re.pattern, re.replacer);
        }
        return result;
    }
}
