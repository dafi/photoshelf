package com.ternaryop.photoshelf.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by dave on 29/12/15.
 * Contains data about DOMSelector
 */
@SuppressWarnings("unchecked")
public final class DOMSelector {
    public static final String DEFAULT_CONTAINER_SELECTOR = "a img";
    public static final DOMSelector DEFAULT_SELECTOR = new DOMSelector();

    // the domain regular expression associated to this selector
    private String domainRE;
    // the selector used to locate the image url
    private String image;
    // the selector used to locate the images container
    private String container = DEFAULT_CONTAINER_SELECTOR;
    // the selector used to locate other pages urls
    private String multiPage;
    // the selector used to locate the document title
    private String title;
    // the data used to make a POST request
    private Map<String, String> postData;
    // the PageSelector(s) needed to find the final image url
    private ArrayList<PageSelector> imageChain;

    public DOMSelector() {
    }

    public DOMSelector(String domainRE, Map<String, Object> value) {
        setDomainRE(domainRE);
        setImage((String)value.get("image"));
        setImageChainList((List<Map<String, String>>)value.get("imageChain"));
        setContainer((String)value.get("container"));
        setMultiPage((String)value.get("multiPage"));
        setTitle((String)value.get("title"));
        setPostData((Map<String, String>)value.get("postData"));
    }

    public String getDomainRE() {
        return domainRE;
    }

    private void setDomainRE(String domainRE) {
        this.domainRE = domainRE;
    }

    public String getImage() {
        return image;
    }

    private void setImage(String image) {
        this.image = image;
    }

    public String getContainer() {
        return container;
    }

    private void setContainer(String container) {
        if (container == null) {
            this.container = DEFAULT_CONTAINER_SELECTOR;
        } else {
            this.container = container;
        }
    }

    public String getMultiPage() {
        return multiPage;
    }

    private void setMultiPage(String multiPage) {
        this.multiPage = multiPage;
    }

    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (domainRE != null) {
            sb.append("domainRE = ").append(domainRE);
        }
        if (image != null) {
            sb.append(" image = ").append(image);
        }
        if (container != null) {
            sb.append(" container = ").append(container);
        }
        if (multiPage != null) {
            sb.append(" multiPage = ").append(multiPage);
        }
        if (title != null) {
            sb.append(" title = ").append(title);
        }
        return sb.toString();
    }

    public Map<String, String> getPostData() {
        return postData;
    }

    private void setPostData(Map<String, String> postData) {
        this.postData = postData;
    }

    public List<PageSelector> getImageChainList() {
        return imageChain;
    }

    /**
     * The CSS selector to use to find the imageUrl.
     * @param list if the image link is available inside a url chain the selector can contain map with "pageSel" and "selAttr" keys
     *             pageSel contains the css selector used to select the element
     *             selAttr contains the element's attribute to use to get the image url.
     */
    private void setImageChainList(List<Map<String, String>> list) {
        if (list == null) {
            return;
        }
        imageChain = new ArrayList<>();
        for (Map<String, String> map : list) {
            String pageSel = map.get("pageSel");
            String selAttr = map.get("selAttr");
            if (pageSel != null && selAttr != null) {
                imageChain.add(new PageSelector(pageSel, selAttr));
            } else {
                throw new IllegalArgumentException("Invalid pageSel: " + pageSel + " selAttr " + selAttr);
            }
        }
    }
}
