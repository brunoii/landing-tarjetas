package com.gentleia.landingtarjetas.projection;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentProjectionRepository extends JpaRepository<InstallmentProjection, Long> {

    List<InstallmentProjection> findByProjectedMonthAndActiveTrueOrderByInstallmentNumberAsc(LocalDate projectedMonth);

    List<InstallmentProjection> findBySourceTransactionIdOrderByProjectedMonthAsc(Long sourceTransactionId);
}
