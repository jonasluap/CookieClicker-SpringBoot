package com.basespringboot.service;

import com.basespringboot.dto.TokenResponse;
import com.basespringboot.entity.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {
    public static final long TOKEN_TTL_SECONDS = 3600;
    private final JwtEncoder jwtEncoder;

    public TokenResponse issue(Player player) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cookie-clicker-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_TTL_SECONDS))
                .subject(player.getUsername())
                .claim("playerId", player.getId())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", TOKEN_TTL_SECONDS, player.getId(), player.getUsername());
    }
}
