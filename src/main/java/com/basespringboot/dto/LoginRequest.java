package com.basespringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "demo_fournee") @NotBlank String username,
        @Schema(example = "fournee-demo-2026", format = "password") @NotBlank String password
) {}
