package com.basespringboot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public record PlayerWeather(
        @Column(name = "weather_place", length = 512) String place,
        @Column(name = "weather_temperature") double temperature,
        @Column(name = "weather_code") int code,
        @Column(name = "weather_expires_at") Instant expiresAt
) {
    public int bonusAt(Instant now) {
        if (!now.isBefore(expiresAt)) {
            return 0;
        }
        return switch (code) {
            case 0, 1, 2, 3 -> 1;
            case 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86 -> 2;
            case 95, 96, 99 -> 3;
            default -> 0;
        };
    }
}
