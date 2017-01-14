package com.ternaryop.photoshelf.selector;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;

import com.ternaryop.photoshelf.util.AssetsJsonConfig;
import com.ternaryop.utils.JSONUtils;
import org.json.JSONObject;

import static com.ternaryop.photoshelf.selector.DOMSelector.DEFAULT_SELECTOR;

/**
 * Obtain the DOM selector used to search the image contained inside a given url
 * 
 * @author dave
 * 
 */
public class ImageDOMSelectorFinder extends AssetsJsonConfig {
    private static final String SELECTORS_FILENAME = "domSelectors.json";

    private static final HashMap<String, DOMSelector> domainMap = new HashMap<>();
    private static int version = -1;

    public ImageDOMSelectorFinder(Context context) {
        if (version > 0) {
            return;
        }
        try {
            synchronized (domainMap) {
                if (!domainMap.isEmpty()) {
                    return;
                }
                final JSONObject jsonObject = readAssetsConfig(context, SELECTORS_FILENAME);
                version = readVersion(jsonObject);
                buildSelectors(JSONUtils.toMap(jsonObject.getJSONObject("selectors")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getVersion() {
        return version;
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
