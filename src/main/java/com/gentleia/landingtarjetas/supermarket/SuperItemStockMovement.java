package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "super_stock_movements")
public class SuperItemStockMovement {

    public enum MovementType {
        ADJUSTMENT,
        PURCHASE,
        CONSUMPTION,
        QUICK_CONSUMPTION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private SuperItem item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MovementType movementType;

    @Column(precision = 10, scale = 3)
    private BigDecimal previousStock;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal resultingStock;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(length = 500)
    private String notes;

    @Column(length = 40)
    private String source;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected SuperItemStockMovement() {
    }

    public SuperItemStockMovement(SuperItem item, BigDecimal previousStock, BigDecimal resultingStock) {
        this.item = item;
        this.movementType = MovementType.ADJUSTMENT;
        this.previousStock = previousStock;
        this.resultingStock = resultingStock;
        this.source = "MANUAL";
    }

    public SuperItemStockMovement(SuperItem item, MovementType movementType, BigDecimal previousStock, BigDecimal resultingStock,
            BigDecimal quantity, String notes, String source) {
        this.item = item;
        this.movementType = movementType;
        this.previousStock = previousStock;
        this.resultingStock = resultingStock;
        this.quantity = quantity;
        this.notes = notes;
        this.source = source;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public SuperItem getItem() {
        return item;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public BigDecimal getPreviousStock() {
        return previousStock;
    }

    public BigDecimal getResultingStock() {
        return resultingStock;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getNotes() {
        return notes;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
