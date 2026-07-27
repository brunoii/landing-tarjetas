package com.gentleia.landingtarjetas.supermarket;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
enum SuperInventoryScanSessionState { ACTIVE, CONFIRMED, EXPIRED }
enum SuperInventoryScanLineKind { RESOLVED_ITEM, DRAFT }
enum SuperInventoryScanDraftType { PURCHASE, CONSUMPTION }
@Entity
@Table(name = "super_inventory_scan_sessions")
class SuperInventoryScanSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 128) private String ownerSessionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SuperInventoryScanSessionState state;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected SuperInventoryScanSession() {}
    SuperInventoryScanSession(String ownerSessionId, Instant expiresAt) { this.ownerSessionId = ownerSessionId; this.expiresAt = expiresAt; this.state = SuperInventoryScanSessionState.ACTIVE; }
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    Long getId() { return id; }
    String getOwnerSessionId() { return ownerSessionId; }
    SuperInventoryScanSessionState getState() { return state; }
    Instant getExpiresAt() { return expiresAt; }
    boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
    void renew(Instant expiresAt) { this.expiresAt = expiresAt; }
    void expire(Instant now) { state = SuperInventoryScanSessionState.EXPIRED; expiresAt = now; }
    void confirm(Instant now) { state = SuperInventoryScanSessionState.CONFIRMED; expiresAt = now; }
}
@Entity
@Table(name = "super_inventory_scan_lines")
class SuperInventoryScanLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "session_id", nullable = false) private SuperInventoryScanSession session;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id", nullable = false) private SuperItem item;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SuperInventoryScanLineKind kind;
    @Enumerated(EnumType.STRING) @Column(length = 16) private SuperInventoryScanDraftType draftType;
    @Column(precision = 10, scale = 3) private BigDecimal quantity;
    @Column(length = 500) private String notes;
    @Column(nullable = false) private boolean allowNegativeStock;
    @Column(length = SupermarketLimits.BARCODE_CODE_MAX_LENGTH) private String barcodeCode;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected SuperInventoryScanLine() {}
    static SuperInventoryScanLine resolved(SuperInventoryScanSession session, SuperItem item, String barcodeCode) { var line = new SuperInventoryScanLine(); line.session = session; line.item = item; line.kind = SuperInventoryScanLineKind.RESOLVED_ITEM; line.barcodeCode = barcodeCode; return line; }
    static SuperInventoryScanLine draft(SuperInventoryScanSession session, SuperItem item, SuperInventoryMovementDraftRequest request, String notes) { var line = new SuperInventoryScanLine(); line.session = session; line.item = item; line.kind = SuperInventoryScanLineKind.DRAFT; line.applyDraft(request, notes); return line; }
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    void applyDraft(SuperInventoryMovementDraftRequest request, String notes) { draftType = request.type(); quantity = request.quantity(); this.notes = notes; allowNegativeStock = Boolean.TRUE.equals(request.allowNegativeStock()); }
    Long getId() { return id; }
    SuperInventoryScanLineKind getKind() { return kind; }
    SuperItem getItem() { return item; }
    String getBarcodeCode() { return barcodeCode; }
    SuperInventoryScanDraftType getDraftType() { return draftType; }
    BigDecimal getQuantity() { return quantity; }
    String getNotes() { return notes; }
    boolean isAllowNegativeStock() { return allowNegativeStock; }
}
record SuperInventoryScanResolvedItemRequest(@NotNull Long itemId, @Size(max = SupermarketLimits.BARCODE_CODE_MAX_LENGTH, message = "no puede superar {max} caracteres") String barcodeCode) {}
record SuperInventoryMovementDraftRequest(@NotNull Long itemId, @NotNull SuperInventoryScanDraftType type,
        @NotNull @Digits(integer = 7, fraction = 3, message = "debe tener hasta 7 enteros y 3 decimales") @DecimalMin(value = "0.0", inclusive = false, message = "debe ser mayor a 0") BigDecimal quantity,
        @Size(max = 500, message = "no puede superar {max} caracteres") String notes, Boolean allowNegativeStock) {}
record SuperInventoryScanResolvedItemResponse(Long id, Long itemId, String itemName, String barcodeCode) { static SuperInventoryScanResolvedItemResponse from(SuperInventoryScanLine line) { return new SuperInventoryScanResolvedItemResponse(line.getId(), line.getItem().getId(), line.getItem().getName(), line.getBarcodeCode()); } }
record SuperInventoryMovementDraftResponse(Long id, Long itemId, String itemName, SuperInventoryScanDraftType type, BigDecimal quantity, String notes, boolean allowNegativeStock) { static SuperInventoryMovementDraftResponse from(SuperInventoryScanLine line) { return new SuperInventoryMovementDraftResponse(line.getId(), line.getItem().getId(), line.getItem().getName(), line.getDraftType(), line.getQuantity(), line.getNotes(), line.isAllowNegativeStock()); } }
record SuperInventoryScanSessionResponse(Long id, String state, Instant expiresAt, List<SuperInventoryScanResolvedItemResponse> resolvedItems, List<SuperInventoryMovementDraftResponse> drafts) {
    static SuperInventoryScanSessionResponse from(SuperInventoryScanSession session, List<SuperInventoryScanLine> lines) {
        return new SuperInventoryScanSessionResponse(session.getId(), session.getState().name(), session.getExpiresAt(),
                lines.stream().filter(line -> line.getKind() == SuperInventoryScanLineKind.RESOLVED_ITEM).map(SuperInventoryScanResolvedItemResponse::from).toList(),
                lines.stream().filter(line -> line.getKind() == SuperInventoryScanLineKind.DRAFT).map(SuperInventoryMovementDraftResponse::from).toList());
    }
}
record SuperInventoryScanConfirmedMovementResponse(Long itemId, String itemName, String movementType, BigDecimal quantity,
        BigDecimal resultingStock, String source) {
    static SuperInventoryScanConfirmedMovementResponse from(SuperItemStockMovement movement) {
        return new SuperInventoryScanConfirmedMovementResponse(movement.getItem().getId(), movement.getItem().getName(),
                movement.getMovementType().name(), movement.getQuantity(), movement.getResultingStock(), movement.getSource());
    }
}
record SuperInventoryScanConfirmResponse(SuperInventoryScanSessionResponse session,
        List<SuperInventoryScanConfirmedMovementResponse> movements) {}
interface SuperInventoryScanSessionRepository extends JpaRepository<SuperInventoryScanSession, Long> {
    Optional<SuperInventoryScanSession> findByOwnerSessionIdAndState(String ownerSessionId, SuperInventoryScanSessionState state);
    Optional<SuperInventoryScanSession> findByIdAndOwnerSessionIdAndState(Long id, String ownerSessionId, SuperInventoryScanSessionState state);
    Optional<SuperInventoryScanSession> findByIdAndOwnerSessionId(Long id, String ownerSessionId);
}
interface SuperInventoryScanLineRepository extends JpaRepository<SuperInventoryScanLine, Long> {
    long countBySessionId(Long sessionId);
    List<SuperInventoryScanLine> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);
    Optional<SuperInventoryScanLine> findByIdAndSessionId(Long id, Long sessionId);
}
