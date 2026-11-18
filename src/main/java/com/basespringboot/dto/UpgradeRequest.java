package com.basespringboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpgradeRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 60, message = "Name must be at most 60 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @Positive(message = "Price must be positive")
        long price,

        @PositiveOrZero(message = "Click bonus must be non-negative")
        long clickBonus,

        @PositiveOrZero(message = "Passive bonus must be non-negative")
        long passiveBonus
) {
}
