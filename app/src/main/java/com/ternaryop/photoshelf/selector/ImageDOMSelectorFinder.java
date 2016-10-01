package com.ternaryop.photoshelf.selector;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
    private static final HashMap<String, DOMSelector> domainMap = new HashMap<>();
    private static boolean isUpgraded;
    private static final String SELECTORS_FILENAME = "domSelectors.json";
    private static final DOMSelector DEFAULT_SELECTOR = new DOMSelector() {
        @Override
        public String getContainer() {
            return DEFAULT_CONTAINER_SELECTOR;
        }
        @Override
        public void setDomainRE(String domainRE) {
            throw new RuntimeException("Readonly instance");
        }
        @Override
        public void setImage(String image) {
            throw new RuntimeException("Readonly instance");
        }
        @Override
        public void setMultiPage(String multiPage) {
            throw new RuntimeException("Readonly instance");
        }
        @Override
        public void setTitle(String title) {
            throw new RuntimeException("Readonly instance");
        }
        @Override
        public void setContainer(String container) {
            throw new RuntimeException("Readonly instance");
        }
    };

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
                        buildSelectors(JSONUtils.toMap(jsonPrivate.getJSONObject("selectors")));
                        return;
                    }
                } catch (FileNotFoundException ex) {
                    if (is != null) try { is.close(); is = null; } catch (Exception ignored) {}
                    is = context.getAssets().open(SELECTORS_FILENAME);
                }
                JSONObject jsonAssets = JSONUtils.jsonFromInputStream(is);
                buildSelectors(JSONUtils.toMap(jsonAssets.getJSONObject("selectors")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void buildSelectors(Map<String, Object> selectors) {
        for (String key : selectors.keySet()) {
            domainMap.put(key, new DOMSelector(key, (Map<String, Object>)selectors.get(key)));
        }
    }

    public DOMSelector getSelectorFromUrl(String url) {
        if (url != null) {
            for (String domainRE : domainMap.keySet()) {
                if (Pattern.compile(domainRE).matcher(url).find()) {
                    return domainMap.get(domainRE);
                }
            }
        }
        return DEFAULT_SELECTOR;
    }
}
