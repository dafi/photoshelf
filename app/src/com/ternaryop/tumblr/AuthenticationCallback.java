package com.ternaryop.tumblr;

public interface AuthenticationCallback {
	public void authenticated(String token, String tokenSecret, Exception error);
}
