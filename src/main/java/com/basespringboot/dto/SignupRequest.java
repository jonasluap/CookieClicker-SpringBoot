package com.basespringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(example = "demo_fournee")
        @NotBlank @Size(max = 32) String username,
        @Schema(example = "fournee-demo-2026", format = "password")
        @NotBlank @Size(min = 8, max = 72) String password
) {}
