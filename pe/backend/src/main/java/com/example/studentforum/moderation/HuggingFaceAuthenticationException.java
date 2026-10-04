package com.example.studentforum.moderation;

public class HuggingFaceAuthenticationException extends RuntimeException {

    public HuggingFaceAuthenticationException(String message) {
        super(message);
    }

    public HuggingFaceAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
