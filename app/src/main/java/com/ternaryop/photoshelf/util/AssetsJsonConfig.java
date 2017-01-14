package com.ternaryop.photoshelf.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.support.annotation.Nullable;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Read config from json files stored on assets and check if upgraded files are present.
 * The json file must have an integer version field
 * Created by dave on 13/01/17.
 */

public abstract class AssetsJsonConfig {
    protected JSONObject readAssetsConfig(Context context, String fileName) throws IOException, JSONException {
        JSONObject jsonAssets = jsonFromAssets(context, fileName);
        JSONObject jsonPrivate = jsonFromPrivateFile(context, fileName);
        if (jsonPrivate != null) {
            int assetsVersion = readVersion(jsonAssets);
            int privateVersion = readVersion(jsonPrivate);
            if (privateVersion > assetsVersion) {
                return jsonPrivate;
            }
            context.deleteFile(fileName);
        }
        return jsonAssets;
    }

    protected JSONObject jsonFromAssets(Context context, String fileName) throws IOException, JSONException {
        try (InputStream is = context.getAssets().open(fileName)) {
            return JSONUtils.jsonFromInputStream(is);
        }
    }

    @Nullable
    protected JSONObject jsonFromPrivateFile(Context context, String fileName) throws IOException, JSONException {
        try (InputStream is = context.openFileInput(fileName)) {
            return JSONUtils.jsonFromInputStream(is);
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    protected int readVersion(JSONObject jsonObject) {
        // version may be not present on existing json file
        try {
            return jsonObject.getInt("version");
        } catch (JSONException ignored) {
        }
        return -1;
    }

    public abstract int getVersion();
}
