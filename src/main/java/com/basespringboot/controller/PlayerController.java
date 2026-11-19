package com.basespringboot.controller;

import com.basespringboot.dto.PlayerResponse;
import com.basespringboot.dto.PlayerRequest;
import com.basespringboot.dto.PurchaseResponse;
import com.basespringboot.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public PlayerResponse findMine(@AuthenticationPrincipal Jwt jwt) {
        return playerService.findById(((Number) jwt.getClaim("playerId")).longValue());
    }

    @GetMapping("/{id}")
    public PlayerResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.findById(id);
    }

    @PutMapping("/{id}")
    public PlayerResponse update(@PathVariable Long id, @Valid @RequestBody PlayerRequest request, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        playerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/click")
    public PlayerResponse click(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.click(id);
    }

    @PostMapping("/{id}/collect")
    public PlayerResponse collectPassiveCookies(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.collectPassiveCookies(id);
    }

    @PostMapping("/{playerId}/upgrades/{upgradeId}/purchase")
    public PlayerResponse purchaseUpgrade(@PathVariable Long playerId, @PathVariable Long upgradeId, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(playerId, jwt);
        return playerService.purchaseUpgrade(playerId, upgradeId);
    }

    @GetMapping("/{id}/purchases")
    public List<PurchaseResponse> purchaseHistory(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(id, jwt);
        return playerService.purchaseHistory(id);
    }

    @GetMapping("/leaderboard")
    public List<PlayerResponse> leaderboard() {
        return playerService.leaderboard();
    }

    private void requireOwner(Long playerId, Jwt jwt) {
        if (!playerId.equals(((Number) jwt.getClaim("playerId")).longValue())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You can only access your own player account");
        }
    }
}
