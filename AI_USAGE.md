# AI Usage and Traceability

## Overview

AI was used as a development assistant for requirement analysis, implementation, debugging, testing, and documentation.

AI suggestions were reviewed before use and validated through compilation, Swagger/API testing, PostgreSQL checks, and automated tests. Final engineering decisions and implementation ownership remain with the candidate.

## 1. Architecture and Hash Chain

**Task:** Design the tamper-evident audit service.

**AI suggestion:** Use Spring Boot, PostgreSQL, SHA-256 hash chaining, sequence numbers, a genesis hash, canonical JSON, and transactional chain-state locking.

**Decision:** Accepted with modifications.

**Validation:** Created multiple events and verified sequence ordering, previous-hash linkage, and successful chain verification.

## 2. PostgreSQL JSONB Mapping

**Problem:** PostgreSQL returned:

```text
column "payload" is of type jsonb but expression is of type character varying
```

**AI suggestion:** Map structured payloads using Jackson `JsonNode` and Hibernate `@JdbcTypeCode(SqlTypes.JSON)`.

**Decision:** Accepted.

**Validation:** The event creation API returned HTTP 201 and structured JSON payloads were stored successfully.

## 3. Tamper Detection

**Task:** Validate detection of historical database modification.

**AI suggestion:** Modify a protected audit field directly in PostgreSQL and run chain verification again.

**Decision:** Accepted.

**Validation:** Verification reported:

```text
firstInconsistentSequence = 2
violationType = CURRENT_HASH_MISMATCH
```

The behavior was also covered by an automated integration test.

## 4. Retention and Archival

**AI suggestion:** Implement soft archival with archive-batch metadata while retaining records required for complete chain verification.

**Decision:** Accepted as a prototype trade-off.

**Validation:** Eligible events were marked archived and the complete chain remained valid.

**Limitation:** Soft archival is not independent immutable or WORM storage.

## 5. Structured Redaction

**AI suggestion:** Preserve the original integrity-protected event, store redaction metadata separately, and mask selected fields in the read projection.

**Decision:** Accepted with modifications.

A second JSONB issue occurred for `json_paths` and was fixed using `JsonNode` JSON mapping.

**Validation:** The API returned:

```text
accountNumber = [REDACTED]
```

while the original audit chain remained valid.

**Limitation:** The prototype performs logical redaction rather than physical erasure.

## 6. Verifiable Export

**AI suggestion:** Generate an export containing records, filter metadata, a manifest hash, and an HMAC-based signature.

**Decision:** Accepted.

**Validation:**

- Export by `actorId` succeeded.
- Export by `resourceId` succeeded.
- An unchanged export verified successfully.
- A modified export failed verification.
- Existing redactions remained applied in exports.

**Limitation:** The export protects the generated bundle against modification but does not prove cryptographic completeness of a filtered query.

## 7. Scenario C Analysis

**Task:** Analyze the requirement:

```text
Regulators need to be able to audit access to client account data.
```

**AI assistance:** Helped identify ambiguities, stakeholder questions, assumptions, normalized requirements, and technical mappings.

**Decision:** Modified.

**Validation:** The final Scenario C document separates prototype assumptions from confirmed requirements.

## 8. Automated Testing

**Initial suggestion:** Use PostgreSQL Testcontainers.

**Decision:** Rejected for the local environment because Docker was unavailable.

**Replacement:** A dedicated local PostgreSQL test database was used:

```text
auditdb_test
```

**AI assistance:** Helped create automated tests for:

- Valid hash-chain creation
- Direct database tampering
- Retention and archival
- Structured redaction
- Export integrity and tampering
- Concurrent audit writes

**Validation:**

```text
Failures: 0
Errors: 0
BUILD SUCCESS
```

## 9. Decision Summary

### Accepted

- SHA-256 hash chaining
- Deterministic JSON canonicalization
- PostgreSQL `JsonNode` mapping
- Direct database tamper testing
- Separate redaction records
- Export integrity verification
- Concurrent-write testing

### Modified

- Kept the solution as one Spring Boot service.
- Used soft archival for the prototype.
- Used read-time redaction.
- Replaced Testcontainers with local PostgreSQL testing.
- Adjusted generated documentation to match the implemented solution.

### Rejected or Not Implemented

- Docker-based testing in the current environment
- HSM/KMS integration
- Independent WORM storage
- Production identity federation
- Multi-region deployment
- Cryptographic proof of filtered-query completeness

These are recorded as production considerations rather than implemented prototype features.

## Validation Approach

AI-assisted changes were validated through:

- Java compilation
- Maven verification
- JUnit and integration tests
- Swagger API testing
- PostgreSQL inspection
- Direct database tampering
- Redaction checks
- Export tampering
- Concurrent-write testing

AI was used as an engineering assistant. Final implementation decisions, validation, assumptions, and limitations remain the candidate's responsibility.
