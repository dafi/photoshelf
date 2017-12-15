package com.ternaryop.photoshelf.parsers;

/**
 * Created by dave on 04/12/17.
 * The Location Prefix matcher
 */

public interface LocationPrefix {
    boolean hasLocationPrefix(String location);
    String removePrefix(String target);
}
