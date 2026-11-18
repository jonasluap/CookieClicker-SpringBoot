package com.basespringboot.service;

import com.basespringboot.dto.LoginRequest;
import com.basespringboot.dto.SignupRequest;
import com.basespringboot.dto.TokenResponse;
import com.basespringboot.entity.Player;
import com.basespringboot.exception.ConflictException;
import com.basespringboot.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public TokenResponse signup(SignupRequest request) {
        String username = request.username().trim();
        if (playerRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists");
        }
        Player player = new Player();
        player.setUsername(username);
        player.setPasswordHash(passwordEncoder.encode(request.password()));
        return tokenService.issue(playerRepository.save(player));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Player player = playerRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (player.getPasswordHash() == null || !passwordEncoder.matches(request.password(), player.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return tokenService.issue(player);
    }
}
