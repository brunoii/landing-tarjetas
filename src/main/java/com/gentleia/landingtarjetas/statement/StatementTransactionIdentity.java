package com.gentleia.landingtarjetas.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import com.gentleia.landingtarjetas.transaction.StatementTransaction;

final class StatementTransactionIdentity {

    private StatementTransactionIdentity() {
    }

    static String key(CardStatement statement, StatementTransaction transaction) {
        String transactionKey = hasText(transaction.getOperationNumber())
                ? "op|" + normalize(transaction.getOperationNumber()) + "|" + transactionDetails(transaction)
                : "fallback|" + checksumScope(statement) + "|" + transactionDetails(transaction);
        return statementScope(statement) + "|" + transactionKey;
    }

    static boolean sameStatementScope(CardStatement left, CardStatement right) {
        return safe(left.getProvider()).equals(safe(right.getProvider()))
                && safe(left.getCardBrand()).equals(safe(right.getCardBrand()))
                && normalize(left.getCardAlias()).equals(normalize(right.getCardAlias()))
                && safe(left.getPaymentMonth()).equals(safe(right.getPaymentMonth()))
                && safe(left.getClosingDate()).equals(safe(right.getClosingDate()))
                && safe(left.getDueDate()).equals(safe(right.getDueDate()));
    }

    private static String statementScope(CardStatement statement) {
        return String.join("|",
                safe(statement.getProvider()),
                safe(statement.getCardBrand()),
                normalize(statement.getCardAlias()),
                safe(statement.getPaymentMonth()),
                safe(statement.getClosingDate()),
                safe(statement.getDueDate())
        );
    }

    private static String checksumScope(CardStatement statement) {
        if (statement.getUploadedFile() != null && hasText(statement.getUploadedFile().getChecksumSha256())) {
            return normalize(statement.getUploadedFile().getChecksumSha256());
        }
        return statement.getId() == null ? "unsaved" : "statement-" + statement.getId();
    }

    private static String transactionDetails(StatementTransaction transaction) {
        return String.join("|",
                safe(transaction.getTransactionDate()),
                normalize(transaction.getDescription()),
                safe(transaction.getType()),
                amount(transaction.getAmountPesos()),
                amount(transaction.getAmountUsd()),
                safe(transaction.getCurrentInstallment()),
                safe(transaction.getTotalInstallments())
        );
    }

    private static String amount(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String safe(Object value) {
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        return value == null ? "" : value.toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
