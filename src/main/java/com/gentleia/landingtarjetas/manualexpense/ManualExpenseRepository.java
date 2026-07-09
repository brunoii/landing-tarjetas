package com.gentleia.landingtarjetas.manualexpense;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualExpenseRepository extends JpaRepository<ManualExpense, Long> {
    List<ManualExpense> findAllByOrderByStartMonthAscIdAsc();

    boolean existsByCategoryId(Long categoryId);
}
