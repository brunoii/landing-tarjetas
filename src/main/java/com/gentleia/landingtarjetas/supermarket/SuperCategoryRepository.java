package com.gentleia.landingtarjetas.supermarket;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SuperCategoryRepository extends JpaRepository<SuperCategory, Long> {

    List<SuperCategory> findByActiveTrueOrderByNameAsc();

    Optional<SuperCategory> findByNameIgnoreCase(String name);
}
