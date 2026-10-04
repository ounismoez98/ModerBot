package com.example.studentforum.moderation;

public class HuggingFaceUnavailableException extends RuntimeException {

    public HuggingFaceUnavailableException(String message) {
        super(message);
    }

    public HuggingFaceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
