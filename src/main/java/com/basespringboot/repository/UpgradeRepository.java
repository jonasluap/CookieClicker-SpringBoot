package com.basespringboot.repository;

import com.basespringboot.entity.Upgrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpgradeRepository extends JpaRepository<Upgrade, Long> {

    boolean existsByNameIgnoreCase(String name);
}
