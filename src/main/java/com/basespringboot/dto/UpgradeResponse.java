package com.basespringboot.dto;

public record UpgradeResponse(
        Long id,
        String name,
        String description,
        long price,
        long clickBonus,
        long passiveBonus
) {
}
