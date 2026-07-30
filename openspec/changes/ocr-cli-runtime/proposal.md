# Proposal: OCR CLI Runtime

## Intent

Replace the Linux-fragile Tess4J/JNA OCR runtime with a local `tesseract` CLI adapter that preserves the current upload/service contract while reducing native runtime failures and sanitizing operational errors.

## Scope

### In Scope
- Replace `Tess4jTicketOcrEngine` with a secure CLI-backed `TicketOcrEngine`.
- Introduce a mockable process executor seam for command invocation, timeout, stderr redaction, and bounded temp-file handling.
- Define cleanup, timeout, and operator-documentation expectations for the new runtime.

### Out of Scope
- OCR accuracy tuning, language-model expansion, or receipt parsing changes.
- API/UI contract changes beyond preserving current OCR outcomes.
- Infra automation, package installation, CI image changes, or production rollout scripting.

## Capabilities

### New Capabilities
- `ticket-ocr-runtime`: Secure CLI-based OCR execution for supermarket ticket uploads, including timeout, cleanup, and redacted runtime-failure handling.

### Modified Capabilities
- None.

## Approach

Keep `TicketOcrService` and `TicketOcrEngine` as the stable boundary. Add a CLI adapter plus injected executor/temp-file helper, map non-zero exit/timeout/process failures to sanitized runtime-unavailable outcomes, and remove Tess4J/JNA-specific runtime assumptions from docs.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/Tess4jTicketOcrEngine.java` | Removed | Retire Tess4J/JNA adapter. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrEngine.java` | Modified | Preserve seam for CLI-backed runtime. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/TicketOcrService.java` | Modified | Preserve safe outcome classification with CLI failure mapping. |
| `pom.xml` | Modified | Remove Tess4J/JNA runtime dependencies. |
| `README.md` | Modified | Document local `tesseract` setup, timeout behavior, and safe diagnostics. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Temp files leak on failure/timeout | Med | Require `finally` cleanup and bounded temp lifecycle. |
| Sensitive stderr/path details leak | Med | Redact process output before logs/responses. |
| CLI failures look like empty OCR | Med | Keep explicit runtime-unavailable mapping. |

## Rollback Plan

Revert to the previous `TicketOcrEngine` implementation, restore Tess4J/JNA dependencies, and reinstate prior runtime setup docs if the CLI path proves unstable.

## Dependencies

- Local `tesseract` binary available on the host runtime.

## Success Criteria

- [ ] Proposal-to-spec contract clearly defines the new `ticket-ocr-runtime` capability.
- [ ] Runtime replacement preserves existing OCR outcome categories while removing Tess4J/JNA runtime dependence.
- [ ] Docs explain secure local CLI requirements, timeout behavior, and sanitized operator troubleshooting.
