package com.basespringboot.config;

import com.basespringboot.entity.Upgrade;
import com.basespringboot.repository.UpgradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Provides a small playable catalogue on a fresh database without overwriting custom upgrades. */
@Component
@RequiredArgsConstructor
public class UpgradeCatalogInitializer implements CommandLineRunner {
    private final UpgradeRepository upgradeRepository;

    @Override
    public void run(String... args) {
        if (upgradeRepository.count() > 0) return;

        upgradeRepository.saveAll(List.of(
                upgrade("Curseur en bois", "Un petit coup de pouce à chaque clic.", 15, 1, 0),
                upgrade("Grand bol", "Une meilleure recette pour votre fournée.", 100, 5, 0),
                upgrade("Four magique", "Des cookies tout chauds, même sans cliquer.", 500, 0, 5),
                upgrade("Atelier de pâtisserie", "La production tourne à plein régime.", 2_000, 10, 20),
                upgrade("Usine à cookies", "Une véritable révolution gourmande.", 10_000, 50, 100)
        ));
    }

    private Upgrade upgrade(String name, String description, long price, long clickBonus, long passiveBonus) {
        Upgrade upgrade = new Upgrade();
        upgrade.setName(name);
        upgrade.setDescription(description);
        upgrade.setPrice(price);
        upgrade.setClickBonus(clickBonus);
        upgrade.setPassiveBonus(passiveBonus);
        return upgrade;
    }
}
