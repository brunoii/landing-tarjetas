package com.gentleia.landingtarjetas.supermarket;

import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "super_price_sources", indexes = {
        @Index(name = "uk_super_price_sources_normalized_key", columnList = "normalized_key", unique = true),
        @Index(name = "idx_super_price_sources_active_name", columnList = "active, name")
})
public class SuperPriceSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = SupermarketLimits.PRICE_SOURCE_NAME_MAX_LENGTH)
    private String name;

    @Column(name = "normalized_key", nullable = false, unique = true, length = SupermarketLimits.PRICE_SOURCE_NAME_MAX_LENGTH)
    private String normalizedKey;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SuperPriceSource() {
    }

    public SuperPriceSource(String name) {
        this.name = name.trim();
        this.normalizedKey = normalizedKeyFor(name);
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    static String normalizedKeyFor(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
