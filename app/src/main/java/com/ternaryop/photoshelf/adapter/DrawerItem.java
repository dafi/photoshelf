package com.ternaryop.photoshelf.adapter;


import android.app.Fragment;

import com.ternaryop.photoshelf.counter.CountRetriever;

public class DrawerItem {
    private String title;
    private Class<? extends Fragment> fragmentClass;
    private boolean counterVisible;
    private CountRetriever countRetriever;

    public DrawerItem(String title) {
        this(title, null, false, null);
    }
    
    public DrawerItem(String title, Class<? extends Fragment> fragmentClass) {
        this(title, fragmentClass, false, null);
    }

    public DrawerItem(String title, Class<? extends Fragment> fragmentClass, boolean showCounter, CountRetriever countRetriever) {
        this.title = title;
        this.fragmentClass = fragmentClass;
        this.counterVisible = showCounter;
        this.countRetriever = countRetriever;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Class<? extends Fragment> getFragmentClass() {
        return fragmentClass;
    }

    public void setFragmentClass(Class<? extends Fragment> fragmentClass) {
        this.fragmentClass = fragmentClass;
    }

    public boolean isHeader() {
        return fragmentClass == null;
    }

    public boolean isCounterVisible() {
        return counterVisible;
    }

    public void setCounterVisible(boolean showCounter) {
        this.counterVisible = showCounter;
    }

    public CountRetriever getCountRetriever() {
        return countRetriever;
    }

    public void setCountRetriever(CountRetriever countRetriever) {
        this.countRetriever = countRetriever;
    }
}