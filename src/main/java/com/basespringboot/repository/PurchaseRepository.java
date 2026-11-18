package com.basespringboot.repository;

import com.basespringboot.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByPlayerIdOrderByPurchasedAtDesc(Long playerId);

    boolean existsByUpgradeId(Long upgradeId);
}
