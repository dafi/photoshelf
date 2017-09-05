package com.ternaryop.feedly;

/**
 * Created by dave on 04/09/17.
 * The feedly token has expired
 */

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
