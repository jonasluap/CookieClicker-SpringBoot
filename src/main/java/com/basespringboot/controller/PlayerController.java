package com.basespringboot.controller;

import com.basespringboot.dto.PlayerRequest;
import com.basespringboot.dto.PlayerResponse;
import com.basespringboot.dto.PurchaseResponse;
import com.basespringboot.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<PlayerResponse> create(@Valid @RequestBody PlayerRequest request) {
        PlayerResponse player = playerService.create(request);
        return ResponseEntity.created(URI.create("/api/players/" + player.id())).body(player);
    }

    @GetMapping
    public List<PlayerResponse> findAll() {
        return playerService.findAll();
    }

    @GetMapping("/{id}")
    public PlayerResponse findById(@PathVariable Long id) {
        return playerService.findById(id);
    }

    @PutMapping("/{id}")
    public PlayerResponse update(@PathVariable Long id, @Valid @RequestBody PlayerRequest request) {
        return playerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        playerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/click")
    public PlayerResponse click(@PathVariable Long id) {
        return playerService.click(id);
    }

    @PostMapping("/{playerId}/upgrades/{upgradeId}/purchase")
    public PlayerResponse purchaseUpgrade(@PathVariable Long playerId, @PathVariable Long upgradeId) {
        return playerService.purchaseUpgrade(playerId, upgradeId);
    }

    @GetMapping("/{id}/purchases")
    public List<PurchaseResponse> purchaseHistory(@PathVariable Long id) {
        return playerService.purchaseHistory(id);
    }

    @GetMapping("/leaderboard")
    public List<PlayerResponse> leaderboard() {
        return playerService.leaderboard();
    }
}
