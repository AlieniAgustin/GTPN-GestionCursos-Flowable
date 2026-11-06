package com.backend.proxy.exception;

public class InvalidCourseStateException extends RuntimeException {

    public InvalidCourseStateException(String message) {
        super(message);
    }

    public InvalidCourseStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
