package com.ternaryop.photoshelf.db;

import java.io.Serializable;
import java.util.Locale;

import android.provider.BaseColumns;

public class Tag implements BaseColumns, Serializable {
    private static final long serialVersionUID = 5887665998065237345L;
    private long id;
    private String name;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name.toLowerCase(Locale.US);
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