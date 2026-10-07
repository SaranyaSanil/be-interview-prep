package com.interviewprep.files;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * An upload that is rejected with a specific status (413 too large, 415 wrong type).
 * As an ErrorResponseException it is rendered by the shared handler in the same ProblemDetail format.
 */
public class FileRejectedException extends ErrorResponseException {

    public FileRejectedException(HttpStatus status, String detail) {
        super(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
