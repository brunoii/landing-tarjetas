package com.gentleia.landingtarjetas.supermarket;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuperItemBarcodeAliasRepository extends JpaRepository<SuperItemBarcodeAlias, Long> {

    @Query("""
            select alias from SuperItemBarcodeAlias alias
            join fetch alias.item item
            join fetch item.category
            where alias.active = true
            and alias.activeCode = :activeCode
            and item.active = true
            """)
    Optional<SuperItemBarcodeAlias> findActiveByActiveCode(String activeCode);

    boolean existsByActiveCodeAndActiveTrue(String activeCode);

    @Modifying(flushAutomatically = true)
    @Query("""
            update SuperItemBarcodeAlias alias
            set alias.active = false,
                alias.activeCode = null,
                alias.deactivatedAt = :deactivatedAt,
                alias.updatedAt = :deactivatedAt
            where alias.active = true
            and alias.item.id = :itemId
            """)
    int deactivateActiveAliasesByItemId(@Param("itemId") Long itemId, @Param("deactivatedAt") Instant deactivatedAt);

    @Modifying(flushAutomatically = true)
    @Query("""
            update SuperItemBarcodeAlias alias
            set alias.active = false,
                alias.activeCode = null,
                alias.deactivatedAt = :deactivatedAt,
                alias.updatedAt = :deactivatedAt
            where alias.active = true
            and alias.activeCode = :activeCode
            and alias.item.id in (
                select item.id from SuperItem item
                where item.active = false
            )
            """)
    int deactivateActiveAliasesForInactiveItemsByActiveCode(
            @Param("activeCode") String activeCode, @Param("deactivatedAt") Instant deactivatedAt);

    Optional<SuperItemBarcodeAlias> findByIdAndItemIdAndActiveTrue(Long id, Long itemId);
}
