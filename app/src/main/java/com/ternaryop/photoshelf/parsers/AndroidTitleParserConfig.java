package com.ternaryop.photoshelf.parsers;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;

import com.ternaryop.photoshelf.util.AssetsJsonConfig;
import org.json.JSONObject;

/**
 * Created by dave on 21/03/15.
 * The Config to use under Android, read the config from assets and upgrade it if an imported version exists
 */
public class AndroidTitleParserConfig extends AssetsJsonConfig implements TitleParserConfig {
    private static final String TITLE_PARSER_FILENAME = "titleParser.json";

    private static int version = -1;
    private static final JSONTitleParserConfig titleParserConfig = new JSONTitleParserConfig();

    public AndroidTitleParserConfig(Context context) {
        if (version > 0) {
            return;
        }

        try {
            final JSONObject jsonObject = readAssetsConfig(context, TITLE_PARSER_FILENAME);
            version = readVersion(jsonObject);
            titleParserConfig.readConfig(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public List<TitleParserRegExp> getTitleCleanerList() {
        return titleParserConfig.getTitleCleanerList();
    }

    @Override
    public String applyList(List<TitleParserRegExp> titleParserRegExpList, String input) {
        return titleParserConfig.applyList(titleParserRegExpList, input);
    }

    @Override
    public Pattern getTitleParserPattern() {
        return titleParserConfig.getTitleParserPattern();
    }

    @Override
    public List<LocationPrefix> getLocationPrefixes() {
        return titleParserConfig.getLocationPrefixes();
    }

    @Override
    public Map<String, Pattern> getCities() {
        return titleParserConfig.getCities();
    }
}
