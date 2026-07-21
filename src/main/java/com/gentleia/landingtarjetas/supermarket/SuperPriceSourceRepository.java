package com.gentleia.landingtarjetas.supermarket;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SuperPriceSourceRepository extends JpaRepository<SuperPriceSource, Long> {

    List<SuperPriceSource> findByActiveTrueOrderByNameAsc();

    Optional<SuperPriceSource> findByNormalizedKey(String normalizedKey);

    Optional<SuperPriceSource> findByIdAndActiveTrue(Long id);
}
