package com.example.studentforum.moderation;

import java.util.List;

public record CommentRequest(String text, List<String> commentaires) {
}
