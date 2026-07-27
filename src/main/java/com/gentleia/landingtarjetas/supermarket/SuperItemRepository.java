package com.gentleia.landingtarjetas.supermarket;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SuperItemRepository extends JpaRepository<SuperItem, Long> {

    @Query("""
            select item from SuperItem item
            join fetch item.category category
            left join fetch item.commercialPresentationPriceSource commercialPresentationPriceSource
            where item.active = true
            order by lower(category.name), lower(item.name)
            """)
    List<SuperItem> findActiveOrderedForList();

    List<SuperItem> findByActiveTrueAndCheckedTrueOrderByNameAsc();

    Optional<SuperItem> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item from SuperItem item
            join fetch item.category
            where item.id = :id
            and item.active = true
            """)
    Optional<SuperItem> findActiveByIdForStockCommand(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item from SuperItem item
            join fetch item.category
            where item.id in :ids
            and item.active = true
            order by item.id asc
            """)
    List<SuperItem> findActiveByIdsForStockCommandOrdered(@Param("ids") List<Long> ids);

    boolean existsByCategoryId(Long categoryId);
}
