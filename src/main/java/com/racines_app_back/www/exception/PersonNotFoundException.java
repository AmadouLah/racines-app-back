package com.racines_app_back.www.exception;

public class PersonNotFoundException extends RuntimeException {
    public PersonNotFoundException(String message) {
        super(message);
    }
    
    public PersonNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
