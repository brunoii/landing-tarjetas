package com.gentleia.landingtarjetas.statement;

import java.time.LocalDate;
import java.util.List;

import com.gentleia.landingtarjetas.shared.CardBrand;
import com.gentleia.landingtarjetas.shared.StatementStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardStatementRepository extends JpaRepository<CardStatement, Long> {

    List<CardStatement> findByPaymentMonthOrderByCardBrandAsc(LocalDate paymentMonth);

    List<CardStatement> findByCardBrandOrderByPaymentMonthDesc(CardBrand cardBrand);

    @Query("""
            select s from CardStatement s
            where (:paymentMonth is null or s.paymentMonth = :paymentMonth)
              and (:cardBrand is null or s.cardBrand = :cardBrand)
            order by s.paymentMonth desc nulls last, s.cardBrand asc
            """)
    List<CardStatement> findWithFilters(@Param("paymentMonth") LocalDate paymentMonth,
                                         @Param("cardBrand") CardBrand cardBrand);

    @Query("""
            select s from CardStatement s
            where s.status = :status
              and (:paymentMonth is null or s.paymentMonth = :paymentMonth)
              and (:cardBrand is null or s.cardBrand = :cardBrand)
            order by s.paymentMonth desc nulls last, s.cardBrand asc
            """)
    List<CardStatement> findWithFiltersAndStatus(@Param("paymentMonth") LocalDate paymentMonth,
                                                 @Param("cardBrand") CardBrand cardBrand,
                                                 @Param("status") StatementStatus status);
}
