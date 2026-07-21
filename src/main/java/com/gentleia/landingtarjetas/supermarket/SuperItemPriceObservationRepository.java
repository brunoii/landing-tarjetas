package com.gentleia.landingtarjetas.supermarket;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SuperItemPriceObservationRepository extends JpaRepository<SuperItemPriceObservation, Long> {

    @Query("""
            select observation from SuperItemPriceObservation observation
            join fetch observation.item item
            left join fetch observation.priceSource priceSource
            order by observation.createdAt desc, observation.id desc
            """)
    List<SuperItemPriceObservation> findRecent(Pageable pageable);

    @Query("""
            select observation from SuperItemPriceObservation observation
            join fetch observation.item item
            left join fetch observation.priceSource priceSource
            where item.id = :itemId
            order by observation.createdAt desc, observation.id desc
            """)
    List<SuperItemPriceObservation> findRecentByItemId(Long itemId, Pageable pageable);
}
