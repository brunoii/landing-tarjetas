package com.gentleia.landingtarjetas.supermarket;

import java.math.BigDecimal;
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

@Entity
@Table(name = "super_items")
public class SuperItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private SuperCategory category;

    @Column(nullable = false, length = SupermarketLimits.ITEM_NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false)
    private boolean checked;

    @Column(length = SupermarketLimits.ITEM_NOTES_MAX_LENGTH)
    private String notes;

    @Column(length = SupermarketLimits.ITEM_UNIT_MAX_LENGTH)
    private String unit;

    @Column(precision = 10, scale = 3)
    private BigDecimal habitualObjective;

    @Column(precision = 10, scale = 3)
    private BigDecimal currentStock;

    @Column(precision = 10, scale = 3)
    private BigDecimal quickQuantity;

    @Column(length = SupermarketLimits.ITEM_PRESENTATION_LABEL_MAX_LENGTH)
    private String commercialPresentationLabel;

    @Column(precision = 10, scale = 3)
    private BigDecimal commercialPresentationQuantity;

    @Column(name = "commercial_presentation_price_pesos", precision = 12, scale = 2)
    private BigDecimal commercialPresentationPricePesos;

    @Column(name = "commercial_presentation_price_source_label", length = SupermarketLimits.ITEM_PRESENTATION_PRICE_SOURCE_LABEL_MAX_LENGTH)
    private String commercialPresentationPriceSourceLabel;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SuperItem() {
    }

    public SuperItem(String name, SuperCategory category) {
        this.name = name;
        this.category = category;
        this.checked = false;
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

    public Long getId() {
        return id;
    }

    public SuperCategory getCategory() {
        return category;
    }

    public void setCategory(SuperCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getHabitualObjective() {
        return habitualObjective;
    }

    public void setHabitualObjective(BigDecimal habitualObjective) {
        this.habitualObjective = habitualObjective;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public BigDecimal getQuickQuantity() {
        return quickQuantity;
    }

    public void setQuickQuantity(BigDecimal quickQuantity) {
        this.quickQuantity = quickQuantity;
    }

    public String getCommercialPresentationLabel() {
        return commercialPresentationLabel;
    }

    public void setCommercialPresentationLabel(String commercialPresentationLabel) {
        this.commercialPresentationLabel = commercialPresentationLabel;
    }

    public BigDecimal getCommercialPresentationQuantity() {
        return commercialPresentationQuantity;
    }

    public void setCommercialPresentationQuantity(BigDecimal commercialPresentationQuantity) {
        this.commercialPresentationQuantity = commercialPresentationQuantity;
    }

    public BigDecimal getCommercialPresentationPricePesos() {
        return commercialPresentationPricePesos;
    }

    public void setCommercialPresentationPricePesos(BigDecimal commercialPresentationPricePesos) {
        this.commercialPresentationPricePesos = commercialPresentationPricePesos;
    }

    public String getCommercialPresentationPriceSourceLabel() {
        return commercialPresentationPriceSourceLabel;
    }

    public void setCommercialPresentationPriceSourceLabel(String commercialPresentationPriceSourceLabel) {
        this.commercialPresentationPriceSourceLabel = commercialPresentationPriceSourceLabel;
    }

    public boolean isConfigured() {
        return unit != null && habitualObjective != null;
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
