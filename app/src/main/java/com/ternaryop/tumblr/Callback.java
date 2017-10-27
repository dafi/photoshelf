package com.ternaryop.tumblr;

public interface Callback<T> {
    /**
     * Used to communicate with tumblr after APIs calls, is always called on main thread
     * @param result some API can return data
     */
    void complete(T result);
    
    /**
     * Called when some error stop execution, is always called on main thread
     * @param e the exception that caused the failure
     */
    void failure(Exception e);
}
