package com.basespringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpgradeRequest(
        @Schema(example = "Mini-four de démonstration")
        @NotBlank(message = "Name is required")
        @Size(max = 60, message = "Name must be at most 60 characters")
        String name,

        @Schema(example = "Ajoute un cookie par seconde.")
        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @Schema(example = "30")
        @Positive(message = "Price must be positive")
        long price,

        @Schema(example = "0")
        @PositiveOrZero(message = "Click bonus must be non-negative")
        long clickBonus,

        @Schema(example = "1")
        @PositiveOrZero(message = "Passive bonus must be non-negative")
        long passiveBonus
) {
}
