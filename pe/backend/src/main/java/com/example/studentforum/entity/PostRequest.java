package com.example.studentforum.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10_000) String content,
        @NotBlank @Size(max = 100) String author
) {
}
