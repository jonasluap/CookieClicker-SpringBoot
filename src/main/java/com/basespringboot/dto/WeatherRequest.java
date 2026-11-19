package com.basespringboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WeatherRequest(
        @Schema(example = "Paris") @NotBlank @Size(max = 80) String city
) {
}
