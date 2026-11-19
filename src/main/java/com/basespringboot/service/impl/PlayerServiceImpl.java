package com.basespringboot.service.impl;

import com.basespringboot.dto.PlayerRequest;
import com.basespringboot.dto.PlayerResponse;
import com.basespringboot.dto.PurchaseResponse;
import com.basespringboot.dto.WeatherResponse;
import com.basespringboot.entity.Player;
import com.basespringboot.entity.PlayerWeather;
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

import java.time.Instant;
import java.util.List;
import java.util.function.ToLongFunction;

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
        Instant now = Instant.now();
        return playerRepository.findAll().stream().map(player -> toResponse(player, now, false)).toList();
    }

    @Override
    public PlayerResponse update(Long id, PlayerRequest request) {
        Player player = requirePlayerForUpdate(id);
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
        return creditCookies(id, Player::getCookiesPerClick);
    }

    @Override
    public PlayerResponse collectPassiveCookies(Long id) {
        return creditCookies(id, Player::getCookiesPerSecond);
    }

    @Override
    public PlayerResponse updateWeather(Long id, PlayerWeather weather) {
        Player player = requirePlayerForUpdate(id);
        // Keep the temporary weather snapshot separate from the permanent upgrade bonuses.
        player.setWeather(weather);
        return toResponse(player);
    }

    @Override
    public PlayerResponse purchaseUpgrade(Long playerId, Long upgradeId) {
        Player player = requirePlayerForUpdate(playerId);
        Upgrade upgrade = upgradeRepository.findById(upgradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade not found: " + upgradeId));

        if (player.getCookies() < upgrade.getPrice()) {
            throw new ConflictException("Not enough cookies to purchase this upgrade");
        }

        // Commit the debit, permanent rate increases and purchase history in one transaction.
        long newClicks = Math.addExact(player.getCookiesPerClick(), upgrade.getClickBonus());
        long newPassive = Math.addExact(player.getCookiesPerSecond(), upgrade.getPassiveBonus());
        player.setCookies(player.getCookies() - upgrade.getPrice());
        player.setCookiesPerClick(newClicks);
        player.setCookiesPerSecond(newPassive);

        // Record the price paid so future catalogue changes leave the receipt accurate.
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
        Instant now = Instant.now();
        return playerRepository.findTop10ByOrderByCookiesDesc().stream()
                .map(player -> toResponse(player, now, false))
                .toList();
    }

    // The caller selects the base rate for either a click or one passive production tick.
    private PlayerResponse creditCookies(Long id, ToLongFunction<Player> baseRate) {
        Player player = requirePlayerForUpdate(id);
        // Evaluate the gain and the returned rates at the same instant in case the bonus expires.
        Instant now = Instant.now();
        int bonus = weatherBonus(player, now);
        long earned = effectiveRate(baseRate.applyAsLong(player), bonus);
        player.setCookies(Math.addExact(player.getCookies(), earned));
        return toResponse(player, now, true);
    }

    private Player requirePlayerForUpdate(Long id) {
        return playerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private Player requirePlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private PlayerResponse toResponse(Player player) {
        return toResponse(player, Instant.now(), true);
    }

    private PlayerResponse toResponse(Player player, Instant now, boolean includeWeather) {
        PlayerWeather weather = player.getWeather();
        int bonus = weatherBonus(player, now);
        // Include the saved city only in responses sent to its owner.
        WeatherResponse weatherResponse = includeWeather && weather != null
                ? new WeatherResponse(weather.place(), weather.temperature(), weather.code(), bonus,
                        !now.isBefore(weather.expiresAt()))
                : null;
        return new PlayerResponse(player.getId(), player.getUsername(), player.getCookies(),
                effectiveRate(player.getCookiesPerClick(), bonus), effectiveRate(player.getCookiesPerSecond(), bonus),
                weatherResponse);
    }

    private static int weatherBonus(Player player, Instant now) {
        return player.getWeather() == null ? 0 : player.getWeather().bonusAt(now);
    }

    private static long effectiveRate(long baseRate, int bonus) {
        return Math.min(baseRate, Long.MAX_VALUE - bonus) + bonus;
    }
}
