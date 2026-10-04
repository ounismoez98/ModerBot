package com.example.studentforum.entity;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostTest {

    @Test
    void createdAtUsesMySqlMicrosecondPrecision() {
        Post post = new Post("Study group", "Meet in the library.", "Alex");

        post.setCreatedAt();

        assertEquals(post.getCreatedAt().truncatedTo(ChronoUnit.MICROS), post.getCreatedAt());
    }

    @Test
    void treatsLegacyPostsWithoutAModerationStatusAsPendingReview() {
        Post post = new Post("Study group", "Meet in the library.", "Alex");
        post.setStatus(null);

        assertEquals(PostStatus.PENDING, post.getStatus());
    }
}
