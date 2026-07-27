package com.gentleia.landingtarjetas.supermarket;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/super/scan-sessions")
public class SuperInventoryScanSessionController {
    private final SuperInventoryScanSessionService service;
    public SuperInventoryScanSessionController(SuperInventoryScanSessionService service) { this.service = service; }
    @GetMapping("/active")
    public SuperInventoryScanSessionResponse active(HttpSession session) { return service.active(session.getId()); }
    @PostMapping("/active")
    public SuperInventoryScanSessionResponse createActive(HttpSession session) { return service.active(session.getId()); }
    @PostMapping("/{id}/resolved-items")
    public SuperInventoryScanSessionResponse addResolvedItem(@PathVariable Long id, HttpSession session, @Valid @RequestBody SuperInventoryScanResolvedItemRequest request) { return service.addResolvedItem(id, session.getId(), request); }
    @PostMapping("/{id}/drafts")
    public SuperInventoryScanSessionResponse createDraft(@PathVariable Long id, HttpSession session, @Valid @RequestBody SuperInventoryMovementDraftRequest request) { return service.createDraft(id, session.getId(), request); }
    @PutMapping("/{id}/drafts/{draftId}")
    public SuperInventoryScanSessionResponse updateDraft(@PathVariable Long id, @PathVariable Long draftId, HttpSession session, @Valid @RequestBody SuperInventoryMovementDraftRequest request) { return service.updateDraft(id, draftId, session.getId(), request); }
    @DeleteMapping("/{id}/drafts/{draftId}")
    public SuperInventoryScanSessionResponse deleteDraft(@PathVariable Long id, @PathVariable Long draftId, HttpSession session) { return service.deleteDraft(id, draftId, session.getId()); }
    @PostMapping("/{id}/confirm")
    public SuperInventoryScanConfirmResponse confirm(@PathVariable Long id, HttpSession session) { return service.confirm(id, session.getId()); }
}
