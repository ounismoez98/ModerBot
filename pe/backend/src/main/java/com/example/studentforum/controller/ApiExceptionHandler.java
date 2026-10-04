package com.example.studentforum.controller;

import com.example.studentforum.service.PostNotFoundException;
import com.example.studentforum.moderation.HuggingFaceAuthenticationException;
import com.example.studentforum.moderation.HuggingFaceUnavailableException;
import com.example.studentforum.moderation.ModerationInputException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePostNotFound(PostNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidModerationState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(ModerationInputException.class)
    public ResponseEntity<Map<String, String>> handleModerationInput(ModerationInputException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(HuggingFaceAuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleHuggingFaceAuthentication(
            HuggingFaceAuthenticationException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(HuggingFaceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleHuggingFaceUnavailable(
            HuggingFaceUnavailableException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .orElse("Les données de la publication sont invalides.");

        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
