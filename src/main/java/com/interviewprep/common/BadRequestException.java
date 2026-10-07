package com.interviewprep.common;

/** A business-rule violation in the request that Bean Validation cannot express. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
