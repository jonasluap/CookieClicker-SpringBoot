package com.basespringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @Schema(example = "chef_fournee")
        @NotBlank(message = "Username is required")
        @Size(max = 32, message = "Username must be at most 32 characters")
        String username
) {
}
