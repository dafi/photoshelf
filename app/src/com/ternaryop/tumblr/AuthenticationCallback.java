package com.ternaryop.tumblr;

public interface AuthenticationCallback {
    public void tumblrAuthenticated(String token, String tokenSecret, Exception error);
}
