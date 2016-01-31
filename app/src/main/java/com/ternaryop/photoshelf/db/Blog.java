package com.ternaryop.photoshelf.db;

import java.io.Serializable;
import java.util.Locale;

import android.provider.BaseColumns;

public class Blog implements BaseColumns, Serializable {
    private static final long serialVersionUID = 5887665998065237345L;
    private long id;
    private String name;

    public Blog() {
    }

    public Blog(long id, String name) {
        this.id = id;
        this.name = name.toLowerCase(Locale.US);
    }

    public Blog(String name) {
        this(0, name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase(Locale.US);
    }

    @Override
    public String toString() {
        return name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}