package com.ternaryop.photoshelf.counter;

public interface CountChangedListener {
    public void onChangeCount(CountProvider provider, long newCount);
}
