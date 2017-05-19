package com.exception;

/**
 * Created by rask on 05.05.2017.
 */
public class DuplicateUsernameException extends Exception {
    public DuplicateUsernameException(String message){
        super(message);
    }
}
