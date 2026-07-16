package com.gentleia.landingtarjetas.supermarket;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "super_item_barcode_aliases",
        uniqueConstraints = @UniqueConstraint(name = "uk_super_item_barcode_aliases_active_code", columnNames = "active_code")
)
public class SuperItemBarcodeAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private SuperItem item;

    @Column(nullable = false, length = SupermarketLimits.BARCODE_CODE_MAX_LENGTH)
    private String code;

    @Column(length = SupermarketLimits.BARCODE_FORMAT_MAX_LENGTH)
    private String format;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "active_code", length = SupermarketLimits.BARCODE_CODE_MAX_LENGTH)
    private String activeCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deactivatedAt;

    protected SuperItemBarcodeAlias() {
    }

    public SuperItemBarcodeAlias(SuperItem item, String code, String format) {
        this.item = item;
        this.code = code;
        this.format = format;
        this.active = true;
        this.activeCode = code;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        syncActiveCode();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
        syncActiveCode();
    }

    public void deactivate() {
        this.active = false;
        this.activeCode = null;
        this.deactivatedAt = Instant.now();
    }

    private void syncActiveCode() {
        this.activeCode = active ? code : null;
    }

    public Long getId() {
        return id;
    }

    public SuperItem getItem() {
        return item;
    }

    public String getCode() {
        return code;
    }

    public String getFormat() {
        return format;
    }

    public boolean isActive() {
        return active;
    }

    public String getActiveCode() {
        return activeCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }
}
