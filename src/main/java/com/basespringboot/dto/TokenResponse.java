package com.basespringboot.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, Long playerId, String username) {}
