# Supermarket Ticket OCR Readiness Specification

## Purpose

Define explicit OCR failure states and a local-only receipt verification policy.

## Requirements

### Requirement: Explicit OCR stage feedback

The system MUST distinguish invalid file type, decode failure, missing OCR runtime/data, and empty OCR extraction without collapsing them into a generic upload error.

#### Scenario: Stage-specific OCR failure
- GIVEN a receipt upload fails at validation, decode, runtime readiness, or extraction
- WHEN the OCR flow returns feedback
- THEN the system MUST identify the failing stage with a clear non-generic outcome

#### Scenario: Invalid type is explicit
- GIVEN the selected file is not a PNG or JPEG ticket image
- WHEN OCR validation rejects the file before extraction
- THEN the API MUST return the `INVALID_FILE` outcome
- AND the UI MUST show a contract-specific guidance message instead of a generic upload failure

#### Scenario: Decode failure is explicit
- GIVEN the selected file declares an allowed image type but cannot be decoded as a ticket image
- WHEN OCR decoding fails before Tess4J runs
- THEN the API MUST return the `DECODE_FAILED` outcome
- AND the UI MUST keep manual review or retry guidance visible

#### Scenario: Runtime readiness failure is explicit
- GIVEN Tess4J runtime, datapath, or language data are unavailable
- WHEN OCR cannot execute safely
- THEN the response MUST use the `RUNTIME_UNAVAILABLE` outcome
- AND the warning text MUST stay sanitized

#### Scenario: Empty extraction is explicit
- GIVEN OCR finishes without any usable date, source, or line candidates
- WHEN the transient review payload is returned
- THEN the response MUST use the `EMPTY_EXTRACTION` outcome
- AND the UI MUST keep manual review guidance visible

#### Scenario: Recoverable fallback remains visible
- GIVEN OCR cannot complete
- WHEN the user reviews the ticket flow
- THEN the system MUST keep manual review or retry paths visible

### Requirement: Local-only JPG verification policy

The system MUST allow manual OCR verification only against files in `C:\Users\BIIbr\Desktop\Proyectos programacion\Proyecto con Gentle-IA\archivosJPG`. Those files MUST NOT be copied, moved, uploaded, cached, persisted, logged, or versioned.

#### Scenario: Authorized local verification
- GIVEN an operator checks OCR behavior with a file from the authorized JPG folder
- WHEN the verification is performed locally
- THEN the system MUST treat that file as local-only evidence
- AND the workflow MUST NOT copy, persist, log, cache, or version the JPG or derived OCR payload

#### Scenario: Persistence attempt is out of contract
- GIVEN a workflow would store or transmit an authorized JPG file
- WHEN that workflow is evaluated
- THEN the system MUST reject that behavior as unsupported

#### Scenario: Local verification can prove the reported stage
- GIVEN an operator uses an authorized local JPG during a local verification session
- WHEN the OCR attempt finishes
- THEN the observed result MUST report `READY`, `RUNTIME_UNAVAILABLE`, or `EMPTY_EXTRACTION`
- AND MUST NOT collapse those outcomes into a generic failure message
