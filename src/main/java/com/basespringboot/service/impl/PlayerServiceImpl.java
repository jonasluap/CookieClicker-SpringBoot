package com.basespringboot.service.impl;

import com.basespringboot.dto.PlayerRequest;
import com.basespringboot.dto.PlayerResponse;
import com.basespringboot.dto.PurchaseResponse;
import com.basespringboot.entity.Player;
import com.basespringboot.entity.Purchase;
import com.basespringboot.entity.Upgrade;
import com.basespringboot.exception.ConflictException;
import com.basespringboot.exception.ResourceNotFoundException;
import com.basespringboot.repository.PlayerRepository;
import com.basespringboot.repository.PurchaseRepository;
import com.basespringboot.repository.UpgradeRepository;
import com.basespringboot.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final UpgradeRepository upgradeRepository;
    private final PurchaseRepository purchaseRepository;

    @Override
    public PlayerResponse create(PlayerRequest request) {
        String username = request.username().trim();
        if (playerRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists");
        }
        Player player = new Player();
        player.setUsername(username);
        return toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse findById(Long id) {
        return toResponse(requirePlayer(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> findAll() {
        return playerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PlayerResponse update(Long id, PlayerRequest request) {
        Player player = requirePlayer(id);
        String username = request.username().trim();
        if (!player.getUsername().equalsIgnoreCase(username)
                && playerRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists");
        }
        player.setUsername(username);
        return toResponse(player);
    }

    @Override
    public void delete(Long id) {
        Player player = requirePlayer(id);
        playerRepository.delete(player); // Cascade removes the player's purchases.
    }

    @Override
    public PlayerResponse click(Long id) {
        Player player = playerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
        player.setCookies(Math.addExact(player.getCookies(), player.getCookiesPerClick()));
        return toResponse(player);
    }

    @Override
    public PlayerResponse purchaseUpgrade(Long playerId, Long upgradeId) {
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));
        Upgrade upgrade = upgradeRepository.findById(upgradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade not found: " + upgradeId));

        if (player.getCookies() < upgrade.getPrice()) {
            throw new ConflictException("Not enough cookies to purchase this upgrade");
        }

        long newClicks = Math.addExact(player.getCookiesPerClick(), upgrade.getClickBonus());
        long newPassive = Math.addExact(player.getCookiesPerSecond(), upgrade.getPassiveBonus());
        player.setCookies(player.getCookies() - upgrade.getPrice());
        player.setCookiesPerClick(newClicks);
        player.setCookiesPerSecond(newPassive);

        Purchase purchase = new Purchase();
        purchase.setPlayer(player);
        purchase.setUpgrade(upgrade);
        purchase.setPricePaid(upgrade.getPrice());
        purchaseRepository.save(purchase);
        return toResponse(player);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseResponse> purchaseHistory(Long playerId) {
        requirePlayer(playerId);
        return purchaseRepository.findByPlayerIdOrderByPurchasedAtDesc(playerId).stream()
                .map(purchase -> new PurchaseResponse(
                        purchase.getId(),
                        purchase.getUpgrade().getId(),
                        purchase.getUpgrade().getName(),
                        purchase.getPricePaid(),
                        purchase.getPurchasedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> leaderboard() {
        return playerRepository.findTop10ByOrderByCookiesDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private Player requirePlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private PlayerResponse toResponse(Player player) {
        return new PlayerResponse(player.getId(), player.getUsername(), player.getCookies(),
                player.getCookiesPerClick(), player.getCookiesPerSecond());
    }
}
