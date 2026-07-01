package com.gentleia.landingtarjetas.projection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.gentleia.landingtarjetas.shared.DateParsers;
import com.gentleia.landingtarjetas.shared.TransactionType;
import com.gentleia.landingtarjetas.statement.CardStatement;
import com.gentleia.landingtarjetas.transaction.StatementTransaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstallmentProjectionService {

    private final InstallmentProjectionRepository projectionRepository;

    public InstallmentProjectionService(InstallmentProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Transactional
    public void replaceForStatement(CardStatement statement) {
        projectionRepository.deleteBySourceStatementId(statement.getId());
        LocalDate baseMonth = DateParsers.normalizeMonth(statement.getPaymentMonth());
        if (baseMonth == null) {
            return;
        }

        List<InstallmentProjection> projections = new ArrayList<>();
        for (StatementTransaction transaction : statement.getTransactions()) {
            projections.addAll(projectionsForTransaction(transaction, baseMonth));
        }

        projectionRepository.saveAll(projections);
    }

    private List<InstallmentProjection> projectionsForTransaction(StatementTransaction transaction, LocalDate baseMonth) {
        if (transaction.getType() != TransactionType.INSTALLMENT
                || transaction.getCurrentInstallment() == null
                || transaction.getTotalInstallments() == null
                || transaction.getCurrentInstallment() >= transaction.getTotalInstallments()) {
            return List.of();
        }

        List<InstallmentProjection> projections = new ArrayList<>();
        for (int installment = transaction.getCurrentInstallment() + 1;
             installment <= transaction.getTotalInstallments();
             installment++) {
            long monthOffset = installment - transaction.getCurrentInstallment();
            InstallmentProjection projection = new InstallmentProjection(
                    transaction,
                    baseMonth.plusMonths(monthOffset),
                    installment,
                    transaction.getTotalInstallments()
            );
            projection.setAmountPesos(transaction.getAmountPesos());
            projection.setAmountUsd(transaction.getAmountUsd());
            projections.add(projection);
        }
        return projections;
    }
}
