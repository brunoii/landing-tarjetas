package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "super_item_price_observations", indexes = {
        @Index(name = "idx_super_price_observations_created_id", columnList = "created_at, id"),
        @Index(name = "idx_super_price_observations_item_created_id", columnList = "item_id, created_at, id"),
        @Index(name = "idx_super_price_observations_price_source", columnList = "price_source_id")
})
public class SuperItemPriceObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private SuperItem item;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePesos;

    @Column(length = SupermarketLimits.ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH)
    private String sourceLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_source_id")
    private SuperPriceSource priceSource;

    @Column
    private LocalDate observedDate;

    @Column(nullable = false, length = SupermarketLimits.ITEM_PRESENTATION_LABEL_MAX_LENGTH)
    private String presentationLabelSnapshot;

    @Column(precision = 10, scale = 3)
    private BigDecimal presentationQuantitySnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SuperItemPriceObservation() {
    }

    public SuperItemPriceObservation(SuperItem item, BigDecimal pricePesos, String sourceLabel, LocalDate observedDate) {
        this(item, pricePesos, sourceLabel, observedDate, null);
    }

    public SuperItemPriceObservation(SuperItem item, BigDecimal pricePesos, String sourceLabel, LocalDate observedDate,
            SuperPriceSource priceSource) {
        this.item = item;
        this.pricePesos = pricePesos;
        this.sourceLabel = sourceLabel;
        this.priceSource = priceSource;
        this.observedDate = observedDate;
        this.presentationLabelSnapshot = item.getCommercialPresentationLabel();
        this.presentationQuantitySnapshot = item.getCommercialPresentationQuantity();
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

    public BigDecimal getPricePesos() {
        return pricePesos;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public SuperPriceSource getPriceSource() {
        return priceSource;
    }

    public LocalDate getObservedDate() {
        return observedDate;
    }

    public String getPresentationLabelSnapshot() {
        return presentationLabelSnapshot;
    }

    public BigDecimal getPresentationQuantitySnapshot() {
        return presentationQuantitySnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
