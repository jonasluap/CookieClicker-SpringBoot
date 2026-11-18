package com.basespringboot.dto;

import java.time.Instant;

public record PurchaseResponse(
        Long id,
        Long upgradeId,
        String upgradeName,
        long pricePaid,
        Instant purchasedAt
) {
}
