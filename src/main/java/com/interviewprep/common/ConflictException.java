package com.interviewprep.common;

/** The request is valid but conflicts with the current state of a resource. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
