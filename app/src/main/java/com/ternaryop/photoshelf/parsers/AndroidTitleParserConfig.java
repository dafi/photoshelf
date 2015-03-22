package com.ternaryop.photoshelf.parsers;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.TextUtils;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONObject;

/**
 * Created by dave on 21/03/15.
 * The Config to use under Android, read the config from assets and upgrade it if an imported version exists
 */
public class AndroidTitleParserConfig implements TitleParserConfig {
    private static final String TITLEPARSER_FILENAME = "titleParser.json";

    private Pattern blackListRegExpr;
    private static boolean isUpgraded;

    public AndroidTitleParserConfig(Context context) {
        InputStream is = null;
        try {
            if (blackListRegExpr != null) {
                return;
            }
            try {
                is = context.openFileInput(TITLEPARSER_FILENAME);
                // if an imported file exists and its version is minor than the file in assets we delete it
                if (!isUpgraded) {
                    isUpgraded = true;
                    JSONObject jsonPrivate = JSONUtils.jsonFromInputStream(is);
                    JSONObject jsonAssets = JSONUtils.jsonFromInputStream(context.getAssets().open(TITLEPARSER_FILENAME));
                    int privateVersion = jsonPrivate.getInt("version");
                    int assetsVersion = jsonAssets.getInt("version");
                    if (privateVersion < assetsVersion) {
                        is.close();
                        context.deleteFile(TITLEPARSER_FILENAME);
                    }
                    createBlackListRegExpr(jsonAssets);
                    return;
                }
            } catch (FileNotFoundException ex) {
                if (is != null) try { is.close(); is = null; } catch (Exception ignored) {}
                is = context.getAssets().open(TITLEPARSER_FILENAME);
            }
            createBlackListRegExpr(JSONUtils.jsonFromInputStream(is));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }

    private void createBlackListRegExpr(JSONObject jsonAssets) throws Exception {
        List<Object> blackList = new ArrayList<Object>();
        blackList.addAll(JSONUtils.toList(jsonAssets.getJSONObject("blackList").getJSONArray("regExprs")));
        blackListRegExpr = Pattern.compile("(" + TextUtils.join("|", blackList) + ")", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public Pattern getBlackListRegExpr() {
        return blackListRegExpr;
    }
}
