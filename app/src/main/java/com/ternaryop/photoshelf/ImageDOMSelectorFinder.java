package com.ternaryop.photoshelf;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.content.Context;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Obtain the DOM selector used to search the image contained inside a given url
 * 
 * @author dave
 * 
 */
public class ImageDOMSelectorFinder {
    private static final HashMap<String, Object> domainMap = new HashMap<>();
    private static final HashMap<String, Object> containerSelectorsMap = new HashMap<>();
    private static final HashMap<String, Object> multiPageSelectorsMap = new HashMap<>();
    private static boolean isUpgraded;
    private static final String SELECTORS_FILENAME = "domSelectors.json";

    public ImageDOMSelectorFinder(Context context) {
        InputStream is = null;
        try {
            synchronized (domainMap) {
                if (!domainMap.isEmpty()) {
                    return;
                }
                try {
                    is = context.openFileInput(SELECTORS_FILENAME);
                    // if an imported file exists and its version is minor than the file in assets we delete it
                    if (!isUpgraded) {
                        isUpgraded = true;
                        JSONObject jsonPrivate = JSONUtils.jsonFromInputStream(is);
                        JSONObject jsonAssets = JSONUtils.jsonFromInputStream(context.getAssets().open(SELECTORS_FILENAME));
                        int privateVersion = -1;

                        // version may be not present on existing json file
                        try {
                            privateVersion = jsonPrivate.getInt("version");
                        } catch (JSONException ignored) {
                        }
                        int assetsVersion = jsonAssets.getInt("version");
                        if (privateVersion <= assetsVersion) {
                            is.close();
                            context.deleteFile(SELECTORS_FILENAME);
                        }
                        domainMap.putAll(JSONUtils.toMap(jsonPrivate.getJSONObject("selectors")));
                        containerSelectorsMap.putAll(JSONUtils.toMap(jsonPrivate.getJSONObject("containerSelectors")));
                        return;
                    }
                } catch (FileNotFoundException ex) {
                    if (is != null) try { is.close(); is = null; } catch (Exception ignored) {}
                    is = context.getAssets().open(SELECTORS_FILENAME);
                }
                JSONObject jsonAssets = JSONUtils.jsonFromInputStream(is);
                domainMap.putAll(JSONUtils.toMap(jsonAssets.getJSONObject("selectors")));
                containerSelectorsMap.putAll(JSONUtils.toMap(jsonAssets.getJSONObject("containerSelectors")));
                multiPageSelectorsMap.putAll(JSONUtils.toMap(jsonAssets.getJSONObject("multiPageSelectors")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }

    public String getSelectorFromUrl(String url) {
        if (url != null) {
            for (String domainRE : domainMap.keySet()) {
                if (Pattern.compile(domainRE).matcher(url).find()) {
                    return domainMap.get(domainRE).toString();
                }
            }
        }
        return null;
    }

    public String getContainerSelectorFromUrl(String url) {
        if (url != null) {
            for (String re : containerSelectorsMap.keySet()) {
                if (Pattern.compile(re).matcher(url).find()) {
                    return containerSelectorsMap.get(re).toString();
                }
            }
        }
        return null;
    }

    public String getMultiPageSelectorFromUrl(String url) {
        if (url != null) {
            for (String re : multiPageSelectorsMap.keySet()) {
                if (Pattern.compile(re).matcher(url).find()) {
                    return multiPageSelectorsMap.get(re).toString();
                }
            }
        }
        return null;
    }
}
