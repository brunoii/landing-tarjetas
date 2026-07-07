package com.gentleia.landingtarjetas.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.category.Category;
import com.gentleia.landingtarjetas.shared.TransactionType;
import com.gentleia.landingtarjetas.statement.CardStatement;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "statement_transactions")
public class StatementTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private CardStatement statement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private LocalDate transactionDate;

    @Column(nullable = false, length = 240)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TransactionType type;

    @Column(precision = 19, scale = 2)
    private BigDecimal amountPesos;

    @Column(precision = 19, scale = 2)
    private BigDecimal amountUsd;

    private Integer currentInstallment;
    private Integer totalInstallments;

    @Column(length = 80)
    private String operationNumber;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected StatementTransaction() {
    }

    public StatementTransaction(CardStatement statement, String description, TransactionType type) {
        this.statement = statement;
        this.description = description;
        this.type = type;
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

    public CardStatement getStatement() {
        return statement;
    }

    public void setStatement(CardStatement statement) {
        this.statement = statement;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmountPesos() {
        return amountPesos;
    }

    public void setAmountPesos(BigDecimal amountPesos) {
        this.amountPesos = amountPesos;
    }

    public BigDecimal getAmountUsd() {
        return amountUsd;
    }

    public void setAmountUsd(BigDecimal amountUsd) {
        this.amountUsd = amountUsd;
    }

    public Integer getCurrentInstallment() {
        return currentInstallment;
    }

    public void setCurrentInstallment(Integer currentInstallment) {
        this.currentInstallment = currentInstallment;
    }

    public Integer getTotalInstallments() {
        return totalInstallments;
    }

    public void setTotalInstallments(Integer totalInstallments) {
        this.totalInstallments = totalInstallments;
    }

    public String getOperationNumber() {
        return operationNumber;
    }

    public void setOperationNumber(String operationNumber) {
        this.operationNumber = operationNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
