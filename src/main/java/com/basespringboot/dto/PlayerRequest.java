package com.basespringboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 32, message = "Username must be at most 32 characters")
        String username
) {
}
