package com.ternaryop.photoshelf.selector;

import java.util.Map;

/**
 * Created by dave on 29/12/15.
 * Contains data about DOMSelector
 */
public class DOMSelector {
    public static final String DEFAULT_CONTAINER_SELECTOR = "a img[src*=jpg]";

    // the domain regular expression associated to this selector
    protected String domainRE;
    // the selector used to locate the image url
    protected String image;
    // the selector used to locate the images container
    protected String container = DEFAULT_CONTAINER_SELECTOR;
    // the selector used to locate other pages urls
    protected String multiPage;
    // the selector used to locate the document title
    protected String title;

    public DOMSelector() {
    }

    public DOMSelector(String domainRE, Map<String, String> value) {
        setDomainRE(domainRE);
        setImage(value.get("image"));
        setContainer(value.get("container"));
        setMultiPage(value.get("multiPage"));
        setTitle(value.get("title"));
    }

    public String getDomainRE() {
        return domainRE;
    }

    public void setDomainRE(String domainRE) {
        this.domainRE = domainRE;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        if (container == null) {
            this.container = DEFAULT_CONTAINER_SELECTOR;
        } else {
            this.container = container;
        }
    }

    public String getMultiPage() {
        return multiPage;
    }

    public void setMultiPage(String multiPage) {
        this.multiPage = multiPage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
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
}