package com.gentleia.landingtarjetas.transaction;

import java.time.LocalDate;
import java.util.List;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.TransactionType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StatementTransactionRepository extends JpaRepository<StatementTransaction, Long> {

    List<StatementTransaction> findByStatementPaymentMonthOrderByTransactionDateAsc(LocalDate paymentMonth);

    List<StatementTransaction> findByStatementCardBrandOrderByTransactionDateAsc(CardBrand cardBrand);

    List<StatementTransaction> findByCategoryIdOrderByTransactionDateAsc(Long categoryId);

    List<StatementTransaction> findByTypeOrderByTransactionDateAsc(TransactionType type);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select t from StatementTransaction t
            join t.statement s
            left join fetch t.category
            where (:paymentMonth is null or s.paymentMonth = :paymentMonth)
              and (:cardBrand is null or s.cardBrand = :cardBrand)
              and (:categoryId is null or t.category.id = :categoryId)
              and (:type is null or t.type = :type)
            order by t.transactionDate asc nulls last, t.id asc
            """)
    List<StatementTransaction> findWithFilters(@Param("paymentMonth") LocalDate paymentMonth,
                                               @Param("cardBrand") CardBrand cardBrand,
                                               @Param("categoryId") Long categoryId,
                                               @Param("type") TransactionType type);
}
