package com.ternaryop.photoshelf.counter;

public interface CountProvider {
    public void setCountChangedListener(CountChangedListener countChangedListener);
    public CountChangedListener getCountChangedListener();
}
