package com.gentleia.landingtarjetas.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gentleia.landingtarjetas.transaction.StatementTransaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "installment_projections")
public class InstallmentProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_transaction_id", nullable = false)
    private StatementTransaction sourceTransaction;

    @Column(nullable = false)
    private LocalDate projectedMonth;
    @Column(nullable = false)
    private int installmentNumber;
    @Column(nullable = false)
    private int totalInstallments;
    @Column(precision = 19, scale = 2)
    private BigDecimal amountPesos;
    @Column(precision = 19, scale = 2)
    private BigDecimal amountUsd;
    @Column(nullable = false)
    private boolean active = true;

    protected InstallmentProjection() {
    }

    public InstallmentProjection(StatementTransaction sourceTransaction, LocalDate projectedMonth,
                                 int installmentNumber, int totalInstallments) {
        this.sourceTransaction = sourceTransaction;
        this.projectedMonth = projectedMonth;
        this.installmentNumber = installmentNumber;
        this.totalInstallments = totalInstallments;
    }

    public Long getId() {
        return id;
    }

    public StatementTransaction getSourceTransaction() {
        return sourceTransaction;
    }

    public LocalDate getProjectedMonth() {
        return projectedMonth;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public int getTotalInstallments() {
        return totalInstallments;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
