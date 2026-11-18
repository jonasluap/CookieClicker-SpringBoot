package com.basespringboot.dto;

public record PlayerResponse(
        Long id,
        String username,
        long cookies,
        long cookiesPerClick,
        long cookiesPerSecond
) {
}
