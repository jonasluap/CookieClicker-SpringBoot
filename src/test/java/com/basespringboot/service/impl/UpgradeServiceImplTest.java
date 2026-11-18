package com.basespringboot.service.impl;

import com.basespringboot.dto.UpgradeRequest;
import com.basespringboot.exception.ConflictException;
import com.basespringboot.repository.PurchaseRepository;
import com.basespringboot.repository.UpgradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpgradeServiceImplTest {
    @Mock
    private UpgradeRepository upgradeRepository;
    @Mock
    private PurchaseRepository purchaseRepository;
    @InjectMocks
    private UpgradeServiceImpl upgradeService;

    @Test
    void upgradeMustHaveARealBonus() {
        assertThrows(IllegalArgumentException.class, () -> upgradeService.create(
                new UpgradeRequest("Empty", "Does nothing", 10, 0, 0)));
    }

    @Test
    void upgradeNameMustBeUnique() {
        when(upgradeRepository.existsByNameIgnoreCase("Cursor")).thenReturn(true);
        assertThrows(ConflictException.class, () -> upgradeService.create(
                new UpgradeRequest("Cursor", "Extra clicks", 10, 1, 0)));
    }
}
