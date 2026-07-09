package com.gentleia.landingtarjetas.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "incomes")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 240)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private IncomeType incomeType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPesos;

    @Column(nullable = false)
    private LocalDate startMonth;

    private LocalDate endMonth;

    @Column(nullable = false)
    private boolean recurringMonthly;

    private Long parentIncomeId;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Income() {
    }

    public Income(String description, IncomeType incomeType, BigDecimal amountPesos, LocalDate startMonth,
                  boolean recurringMonthly) {
        this.description = description;
        this.incomeType = incomeType;
        this.amountPesos = amountPesos;
        this.startMonth = startMonth;
        this.recurringMonthly = recurringMonthly;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncomeType getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(IncomeType incomeType) {
        this.incomeType = incomeType;
    }

    public BigDecimal getAmountPesos() {
        return amountPesos;
    }

    public void setAmountPesos(BigDecimal amountPesos) {
        this.amountPesos = amountPesos;
    }

    public LocalDate getStartMonth() {
        return startMonth;
    }

    public void setStartMonth(LocalDate startMonth) {
        this.startMonth = startMonth;
    }

    public LocalDate getEndMonth() {
        return endMonth;
    }

    public void setEndMonth(LocalDate endMonth) {
        this.endMonth = endMonth;
    }

    public boolean isRecurringMonthly() {
        return recurringMonthly;
    }

    public void setRecurringMonthly(boolean recurringMonthly) {
        this.recurringMonthly = recurringMonthly;
    }

    public Long getParentIncomeId() {
        return parentIncomeId;
    }

    public void setParentIncomeId(Long parentIncomeId) {
        this.parentIncomeId = parentIncomeId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
