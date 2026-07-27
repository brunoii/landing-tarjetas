package com.gentleia.landingtarjetas.supermarket;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class SuperInventoryScanSessionService {
    private static final Duration EXPIRY = Duration.ofHours(2);
    private static final int LINE_LIMIT = 50;
    private final SuperInventoryScanSessionRepository sessionRepository;
    private final SuperInventoryScanLineRepository lineRepository;
    private final SuperItemRepository itemRepository;
    private final SupermarketService supermarketService;
    public SuperInventoryScanSessionService(SuperInventoryScanSessionRepository sessionRepository,
            SuperInventoryScanLineRepository lineRepository,
            SuperItemRepository itemRepository,
            SupermarketService supermarketService) {
        this.sessionRepository = sessionRepository;
        this.lineRepository = lineRepository;
        this.itemRepository = itemRepository;
        this.supermarketService = supermarketService;
    }
    @Transactional
    public SuperInventoryScanSessionResponse active(String ownerSessionId) {
        Instant now = Instant.now();
        SuperInventoryScanSession session = sessionRepository.findByOwnerSessionIdAndState(ownerSessionId, SuperInventoryScanSessionState.ACTIVE)
                .filter(existing -> !expireIfNeeded(existing, now))
                .orElseGet(() -> sessionRepository.save(new SuperInventoryScanSession(ownerSessionId, expiresAt(now))));
        session.renew(expiresAt(now));
        return snapshot(session);
    }
    @Transactional
    public SuperInventoryScanSessionResponse addResolvedItem(Long sessionId, String ownerSessionId, SuperInventoryScanResolvedItemRequest request) {
        SuperInventoryScanSession session = requireActive(sessionId, ownerSessionId);
        ensureCapacity(session);
        lineRepository.save(SuperInventoryScanLine.resolved(session, requireActiveItem(request.itemId()), trim(request.barcodeCode())));
        return snapshot(session);
    }
    @Transactional
    public SuperInventoryScanSessionResponse createDraft(Long sessionId, String ownerSessionId, SuperInventoryMovementDraftRequest request) {
        SuperInventoryScanSession session = requireActive(sessionId, ownerSessionId);
        ensureCapacity(session);
        lineRepository.save(SuperInventoryScanLine.draft(session, requireActiveItem(request.itemId()), request, trim(request.notes())));
        return snapshot(session);
    }
    @Transactional
    public SuperInventoryScanSessionResponse updateDraft(Long sessionId, Long draftId, String ownerSessionId, SuperInventoryMovementDraftRequest request) {
        SuperInventoryScanLine draft = requireDraft(requireActive(sessionId, ownerSessionId), draftId);
        draft.applyDraft(request, trim(request.notes()));
        return snapshot(sessionRepository.getReferenceById(sessionId));
    }
    @Transactional
    public SuperInventoryScanSessionResponse deleteDraft(Long sessionId, Long draftId, String ownerSessionId) {
        SuperInventoryScanLine draft = requireDraft(requireActive(sessionId, ownerSessionId), draftId);
        lineRepository.delete(draft);
        return snapshot(sessionRepository.getReferenceById(sessionId));
    }
    @Transactional
    public SuperInventoryScanConfirmResponse confirm(Long sessionId, String ownerSessionId) {
        SuperInventoryScanSession session = requireConfirmableSession(sessionId, ownerSessionId);
        List<SuperInventoryScanLine> drafts = lineRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId()).stream()
                .filter(line -> line.getKind() == SuperInventoryScanLineKind.DRAFT)
                .sorted(Comparator.comparing((SuperInventoryScanLine line) -> line.getItem().getId()).thenComparing(SuperInventoryScanLine::getId))
                .toList();
        Map<Long, SuperItem> lockedItems = itemRepository.findActiveByIdsForStockCommandOrdered(drafts.stream()
                        .map(line -> line.getItem().getId())
                        .distinct()
                        .toList()).stream()
                .collect(java.util.stream.Collectors.toMap(SuperItem::getId, Function.identity()));
        List<SuperInventoryScanConfirmedMovementResponse> movements = drafts.stream()
                .map(draft -> supermarketService.applyScanSessionMovement(requireLockedItem(lockedItems, draft), draft))
                .map(SuperInventoryScanConfirmedMovementResponse::from)
                .toList();
        session.confirm(Instant.now());
        return new SuperInventoryScanConfirmResponse(snapshot(session), movements);
    }
    private SuperInventoryScanSession requireActive(Long sessionId, String ownerSessionId) {
        Instant now = Instant.now();
        SuperInventoryScanSession session = sessionRepository.findByIdAndOwnerSessionIdAndState(sessionId, ownerSessionId, SuperInventoryScanSessionState.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la sesión de escaneo activa"));
        if (expireIfNeeded(session, now)) throw new ResponseStatusException(HttpStatus.CONFLICT, "La sesión de escaneo expiró");
        session.renew(expiresAt(now));
        return session;
    }
    private boolean expireIfNeeded(SuperInventoryScanSession session, Instant now) {
        if (!session.isExpired(now)) return false;
        session.expire(now);
        return true;
    }
    private SuperInventoryScanSession requireConfirmableSession(Long sessionId, String ownerSessionId) {
        Instant now = Instant.now();
        SuperInventoryScanSession session = sessionRepository.findByIdAndOwnerSessionId(sessionId, ownerSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la sesión de escaneo activa"));
        if (session.getState() == SuperInventoryScanSessionState.CONFIRMED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sesión de escaneo ya fue confirmada");
        if (session.getState() == SuperInventoryScanSessionState.EXPIRED || expireIfNeeded(session, now))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sesión de escaneo expiró");
        session.renew(expiresAt(now));
        return session;
    }
    private void ensureCapacity(SuperInventoryScanSession session) {
        if (lineRepository.countBySessionId(session.getId()) >= LINE_LIMIT)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sesión de escaneo alcanzó el límite de 50 líneas");
    }
    private SuperItem requireLockedItem(Map<Long, SuperItem> lockedItems, SuperInventoryScanLine draft) {
        SuperItem item = lockedItems.get(draft.getItem().getId());
        if (item == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el producto del super");
        return item;
    }
    private SuperInventoryScanLine requireDraft(SuperInventoryScanSession session, Long draftId) {
        SuperInventoryScanLine line = lineRepository.findByIdAndSessionId(draftId, session.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el borrador de movimiento"));
        if (line.getKind() != SuperInventoryScanLineKind.DRAFT)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el borrador de movimiento");
        return line;
    }
    private SuperItem requireActiveItem(Long itemId) {
        return itemRepository.findByIdAndActiveTrue(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el producto del super"));
    }
    private SuperInventoryScanSessionResponse snapshot(SuperInventoryScanSession session) {
        List<SuperInventoryScanLine> lines = lineRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId());
        return SuperInventoryScanSessionResponse.from(session, lines);
    }
    private Instant expiresAt(Instant now) { return now.plus(Duration.ofHours(2)); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
