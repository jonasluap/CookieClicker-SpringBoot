package com.basespringboot.service.impl;

import com.basespringboot.dto.PlayerRequest;
import com.basespringboot.entity.Player;
import com.basespringboot.entity.Purchase;
import com.basespringboot.entity.Upgrade;
import com.basespringboot.exception.ConflictException;
import com.basespringboot.repository.PlayerRepository;
import com.basespringboot.repository.PurchaseRepository;
import com.basespringboot.repository.UpgradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private UpgradeRepository upgradeRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @InjectMocks
    private PlayerServiceImpl playerService;

    private Player player;

    @BeforeEach
    void setup() {
        player = new Player();
        player.setId(1L);
        player.setUsername("demo");
        player.setCookies(10);
        player.setCookiesPerClick(2);
    }

    @Test
    void clickCreditsCookiesPerClick() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));

        var result = playerService.click(1L);

        assertEquals(12, result.cookies());
    }

    @Test
    void purchaseDeductsPriceAndCreditsBonuses() {
        Upgrade upgrade = new Upgrade();
        upgrade.setId(2L);
        upgrade.setName("Cursor");
        upgrade.setPrice(6);
        upgrade.setClickBonus(3);
        upgrade.setPassiveBonus(1);
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(upgradeRepository.findById(2L)).thenReturn(Optional.of(upgrade));

        var result = playerService.purchaseUpgrade(1L, 2L);

        assertEquals(4, result.cookies());
        assertEquals(5, result.cookiesPerClick());
        assertEquals(1, result.cookiesPerSecond());
        verify(purchaseRepository).save(any(Purchase.class));
    }

    @Test
    void purchaseRequiresEnoughCookies() {
        Upgrade upgrade = new Upgrade();
        upgrade.setPrice(20);
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(upgradeRepository.findById(2L)).thenReturn(Optional.of(upgrade));

        assertThrows(ConflictException.class, () -> playerService.purchaseUpgrade(1L, 2L));
    }

    @Test
    void duplicateUsernameIsRejected() {
        when(playerRepository.existsByUsernameIgnoreCase("demo")).thenReturn(true);
        assertThrows(ConflictException.class, () -> playerService.create(new PlayerRequest(" demo ")));
    }
}
