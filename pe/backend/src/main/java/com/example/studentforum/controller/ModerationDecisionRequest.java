package com.example.studentforum.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationDecisionRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
