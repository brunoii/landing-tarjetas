# Ticket OCR Runtime Specification

## Purpose

Define a secure local `tesseract` CLI runtime that preserves current supermarket ticket OCR outcomes while removing Tess4J/JNA runtime dependence.

## Requirements

### Requirement: Secure local CLI execution

The system MUST execute OCR through a local `tesseract` CLI path only and MUST preserve the existing ticket upload/service contract.

#### Scenario: Successful local OCR run
- GIVEN a valid ticket image and an available local `tesseract` runtime
- WHEN OCR completes successfully
- THEN the system MUST return extracted text through the existing OCR boundary

#### Scenario: Unsupported remote runtime behavior
- GIVEN a workflow that depends on a remote OCR service or a changed API contract
- WHEN the runtime behavior is evaluated
- THEN the system MUST reject that behavior as out of contract

### Requirement: Bounded temporary file lifecycle

The system MUST use bounded temporary OCR artifacts and MUST clean them in a `finally`-equivalent path after success, timeout, or failure.

#### Scenario: Cleanup after success
- GIVEN OCR writes temporary runtime artifacts
- WHEN the CLI run completes successfully
- THEN the system MUST remove those temporary artifacts before the request finishes

#### Scenario: Cleanup after timeout or failure
- GIVEN OCR writes temporary runtime artifacts
- WHEN the CLI run times out or fails
- THEN the system MUST still remove those temporary artifacts

### Requirement: Timeout and outcome mapping

The system MUST enforce a bounded OCR runtime timeout. Timeout, process-launch failure, and non-zero CLI exit MUST map to `RUNTIME_UNAVAILABLE`; empty successful extraction MUST map to `EMPTY_EXTRACTION`.

#### Scenario: Timeout becomes runtime unavailable
- GIVEN a valid ticket image and a CLI run that exceeds the allowed runtime
- WHEN OCR is classified
- THEN the system MUST return `RUNTIME_UNAVAILABLE`

#### Scenario: Empty successful extraction remains distinct
- GIVEN a valid ticket image and a successful CLI run with no extracted text
- WHEN OCR is classified
- THEN the system MUST return `EMPTY_EXTRACTION`

### Requirement: Redacted failure diagnostics

The system MUST redact stderr, filesystem paths, and sensitive process details before logs, warnings, or responses are exposed to operators or clients.

#### Scenario: Non-zero exit with sensitive stderr
- GIVEN the CLI returns stderr containing local paths or sensitive runtime details
- WHEN the failure is reported
- THEN the exposed diagnostics MUST omit or redact those details

#### Scenario: Safe operator troubleshooting
- GIVEN the OCR runtime is unavailable
- WHEN an operator reviews the failure signal
- THEN the system MUST provide a sanitized runtime-unavailable indication

### Requirement: Deterministic runtime verification

The system SHOULD support deterministic tests for success, timeout, non-zero exit, redaction, and cleanup without requiring a host `tesseract` installation.

#### Scenario: Mocked timeout verification
- GIVEN automated tests run in an environment without the local OCR binary
- WHEN timeout behavior is verified
- THEN the tests SHOULD prove the same `RUNTIME_UNAVAILABLE` mapping deterministically

#### Scenario: Mocked cleanup and redaction verification
- GIVEN automated tests simulate CLI failure output and temporary artifacts
- WHEN failure handling is verified
- THEN the tests SHOULD prove cleanup and redaction behavior deterministically
