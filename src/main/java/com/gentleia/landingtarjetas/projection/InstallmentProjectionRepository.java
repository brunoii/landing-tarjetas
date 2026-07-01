package com.gentleia.landingtarjetas.projection;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstallmentProjectionRepository extends JpaRepository<InstallmentProjection, Long> {

    List<InstallmentProjection> findBySourceTransactionIdOrderByProjectedMonthAsc(Long sourceTransactionId);

    @Modifying
    @Query("""
            delete from InstallmentProjection p
            where p.sourceTransaction.statement.id = :statementId
            """)
    void deleteBySourceStatementId(@Param("statementId") Long statementId);

    @Query("""
            select distinct p.projectedMonth from InstallmentProjection p
            where p.active = true
            order by p.projectedMonth asc
            """)
    List<LocalDate> findActiveProjectedMonths();

    @Query("""
            select p from InstallmentProjection p
            join fetch p.sourceTransaction t
            join fetch t.statement s
            left join fetch t.category
            where p.active = true
              and p.projectedMonth = :projectedMonth
            order by s.cardBrand asc, t.description asc, p.installmentNumber asc
            """)
    List<InstallmentProjection> findActiveDetailByProjectedMonth(@Param("projectedMonth") LocalDate projectedMonth);
}
