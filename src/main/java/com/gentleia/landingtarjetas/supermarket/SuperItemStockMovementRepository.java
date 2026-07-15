package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SuperItemStockMovementRepository extends JpaRepository<SuperItemStockMovement, Long> {

    @Query("""
            select movement from SuperItemStockMovement movement
            join fetch movement.item item
            order by movement.createdAt desc, movement.id desc
            """)
    List<SuperItemStockMovement> findRecent(Pageable pageable);

    @Query("""
            select movement from SuperItemStockMovement movement
            join fetch movement.item item
            where item.id = :itemId
            order by movement.createdAt desc, movement.id desc
            """)
    List<SuperItemStockMovement> findRecentByItemId(Long itemId, Pageable pageable);
}
