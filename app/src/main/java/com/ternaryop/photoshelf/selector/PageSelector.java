package com.ternaryop.photoshelf.selector;

/**
 * Created by dave on 09/01/17.
 * Contain the data to determine the selector for an page containing an element.
 * The element can be a link to another page.
 */
public class PageSelector {
    public final String selector;
    public final String selAttr;

    public PageSelector(String selector, String selAttr) {
        this.selector = selector;
        this.selAttr = selAttr;
    }
}
