package com.basespringboot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
@Getter
@Setter
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    // Nullable only to keep existing Sprint 1 rows readable; all new accounts require a hash.
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private long cookies = 0;

    @Column(nullable = false)
    private long cookiesPerClick = 1;

    @Column(nullable = false)
    private long cookiesPerSecond = 0;

    @Embedded
    private PlayerWeather weather;

    // One player can own many purchases (One-to-Many).
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Purchase> purchases = new ArrayList<>();
}
