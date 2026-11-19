package com.basespringboot.repository;

import com.basespringboot.entity.Player;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    boolean existsByUsernameIgnoreCase(String username);

    Optional<Player> findByUsernameIgnoreCase(String username);

    List<Player> findTop10ByOrderByCookiesDesc();

    // Serialize account and production changes for the same player.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.id = :id")
    Optional<Player> findByIdForUpdate(@Param("id") Long id);
}
