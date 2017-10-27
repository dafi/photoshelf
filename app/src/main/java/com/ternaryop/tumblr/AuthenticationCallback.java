package com.ternaryop.tumblr;

public interface AuthenticationCallback {
    void tumblrAuthenticated(String token, String tokenSecret, Exception error);
}
