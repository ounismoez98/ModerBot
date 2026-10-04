package com.example.studentforum.service;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(Long id) {
        super("Publication " + id + " introuvable.");
    }
}
