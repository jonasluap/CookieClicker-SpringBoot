package com.basespringboot.service;

import com.basespringboot.dto.PlayerRequest;
import com.basespringboot.dto.PlayerResponse;
import com.basespringboot.dto.PurchaseResponse;

import java.util.List;

public interface PlayerService {
    PlayerResponse create(PlayerRequest request);
    PlayerResponse findById(Long id);
    List<PlayerResponse> findAll();
    PlayerResponse update(Long id, PlayerRequest request);
    void delete(Long id);
    PlayerResponse click(Long id);
    PlayerResponse purchaseUpgrade(Long playerId, Long upgradeId);
    List<PurchaseResponse> purchaseHistory(Long playerId);
    List<PlayerResponse> leaderboard();
}
