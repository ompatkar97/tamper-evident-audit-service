# Final Engineering Summary

## 1. Overview

This project implements a tamper-evident Audit Log Service using Java, Spring Boot, and PostgreSQL.

The objective was to provide a reliable audit trail that can record security-relevant events, detect unauthorized modification of historical records, support operational requirements such as retention and redaction, and provide verifiable audit exports.

The implementation covers the three assignment scenarios:

- Scenario A: Core tamper-evident audit logging
- Scenario B: Retention, structured redaction, and verifiable export
- Scenario C: Analysis and refinement of an ambiguous regulatory requirement

The solution was intentionally implemented as a single Spring Boot service to keep the prototype understandable, testable, and easy to demonstrate.

---

## 2. Architecture

The service uses Spring Boot as the application framework and PostgreSQL as the persistence layer.

The main responsibilities are separated into components for audit-event processing, cryptographic operations, retention/archive operations, structured redaction, export generation, and verification.

Flyway is used to manage the database schema, while Spring Data JPA provides persistence and query support.

Spring Security provides role-based protection for sensitive API operations.

The architecture favors clarity and correctness over unnecessary distribution or infrastructure complexity.

---

## 3. Tamper-Evident Audit Design

Each audit event is assigned a monotonically increasing sequence number and cryptographically linked to the previous event.

A SHA-256 hash is calculated using the integrity-protected event fields together with the previous event hash.

The first event references a predefined genesis hash.

Structured JSON payloads are canonicalized before hashing so that logically equivalent JSON objects produce deterministic input to the hashing operation.

Length-prefixed framing is used when combining hash inputs to avoid ambiguous concatenation.

The service provides chain verification that checks sequence continuity, previous-hash relationships, and recalculated event hashes.

Verification stops at the first detected inconsistency and reports the affected record and violation type.

---

## 4. Scenario A: Core Audit Logging

Scenario A provides the core audit-event functionality.

The implementation supports creation of audit events containing event type, actor identifier, resource information, structured JSON payload, and event timestamp.

Audit records can be queried using combinations of actor, resource, event type, and timestamp criteria with pagination.

Historical audit records are not exposed through general update or delete APIs.

The hash chain was validated using multiple sequential audit events.

Tamper detection was also validated by modifying a historical database record directly outside the application. The verification process correctly identified the first inconsistent record and reported a current-hash mismatch.

This confirms that unauthorized modification of integrity-protected historical event data can be detected.

---

## 5. Scenario B: Retention and Archival

Retention is implemented using soft archival.

Eligible events are identified using a cutoff timestamp and associated with an archive batch. The archive operation records metadata describing the archived range and marks the selected events as archived.

The underlying audit records remain available to the integrity-verification process.

This allows legitimate archival to occur without being incorrectly reported as hash-chain tampering.

The approach was selected as a practical prototype design. A production implementation may move archived records to independently controlled immutable or WORM storage and maintain trusted integrity checkpoints.

---

## 6. Scenario B: Structured Redaction

Structured redaction was designed so that privacy-related masking does not require rewriting the original integrity-protected audit event.

Redaction requests are stored separately from the original event and identify the fields that should be masked.

Normal API responses apply a redacted projection of the payload. For example, an account number can be returned as `[REDACTED]` while unrelated payload fields remain unchanged.

The original event remains unchanged for cryptographic verification.

An HMAC-based commitment is associated with the redaction evidence.

This design allows chain verification to remain valid after an authorized redaction.

The primary limitation is that this is logical redaction rather than irreversible physical erasure. The original sensitive value remains within the protected underlying audit record.

---

## 7. Scenario B: Verifiable Export

The service supports bulk export by actor identifier or resource identifier.

An export contains the matching audit records together with export metadata, a manifest hash, and an HMAC-based signature.

Export verification recomputes the integrity information and determines whether the generated bundle has been modified.

Testing demonstrated that an unchanged export verifies successfully while modification of an exported event causes verification to fail.

Existing redactions are preserved during export so that the export path does not bypass field masking.

The prototype guarantees integrity of the generated export under its key-trust assumptions. It does not claim cryptographic proof that every possible record matching a filtered query was included.

---

## 8. Scenario C: Requirement Analysis

Scenario C started with the ambiguous statement that regulators need to be able to audit access to client account data.

Rather than treating that statement as a complete technical requirement, the analysis identified unclear areas such as the definition of access, actor types, resource scope, privacy expectations, retention requirements, regulator access methods, and required integrity guarantees.

The resulting document separates stakeholder questions, prototype assumptions, normalized requirements, technical design implications, implemented scope, and intentionally excluded functionality.

This prevents prototype assumptions from being represented as confirmed regulatory requirements.

The complete analysis is documented in `SCENARIO_C_REQUIREMENTS.md`.

---

## 9. Concurrency and Transaction Safety

Audit-event creation must preserve a single ordered chain even when several requests arrive concurrently.

The implementation uses transactional locking of shared chain state before allocating the next sequence number and previous hash.

This prevents competing writers from creating duplicate sequence numbers or chain forks.

Automated concurrency testing validates unique sequence numbers, continuous ordering, correct hash relationships, and successful final verification.

The trade-off is that serialization of chain-state updates can limit throughput under very high write volume.

For this prototype, deterministic global ordering and integrity were prioritized over maximum write throughput.

---

## 10. Security

The prototype separates operations using application roles for audit writing, reading, administration, privacy operations, and auditor functions.

Sensitive administrative operations such as archival and redaction are therefore separated from ordinary audit writing or reading.

The current authentication mechanism is intended for demonstration purposes.

A production deployment should integrate enterprise identity, transport security, managed secrets, least-privilege database access, key rotation, and centralized monitoring.

Cryptographic secrets should be managed through dedicated secret-management or key-management infrastructure rather than stored directly in source code or configuration files.

---

## 11. Testing and Validation

The implementation was validated through both manual testing and automated tests.

Manual validation included event creation, querying, clean-chain verification, direct database tampering, archival, structured redaction, export generation, export verification, and intentional modification of exported records.

Automated testing covers cryptographic processing, valid chain construction, historical tampering detection, archival behavior, structured redaction, export integrity, export tampering, and concurrent writes.

Integration tests use a dedicated PostgreSQL test database so destructive integrity tests do not modify normal development data.

The project was successfully validated through the Maven verification lifecycle with all configured tests passing.

---

## 12. AI-Assisted Engineering

AI was used as a development assistant for architecture suggestions, implementation support, debugging, test generation, requirement analysis, and documentation.

AI-generated output was not assumed to be correct automatically.

Suggestions were reviewed against the assignment requirements and validated through compilation, API execution, PostgreSQL behavior, manual integrity tests, and automated testing.

Several AI suggestions were modified to match the actual environment. For example, the original Testcontainers-based integration-test approach was replaced with a dedicated local PostgreSQL test database because Docker was unavailable.

Detailed AI-assisted decisions and validation evidence are documented in `AI_USAGE.md`.

---

## 13. Key Trade-offs and Limitations

The implementation intentionally distinguishes prototype guarantees from stronger production guarantees.

The hash chain provides tamper evidence but is not an independent external trust anchor. A sufficiently privileged attacker who can rewrite historical records and recompute subsequent hashes requires stronger protection such as signed external checkpoints.

Retention currently uses soft archival rather than independent immutable storage.

Redaction is logical masking rather than guaranteed physical deletion of sensitive information.

HMAC-based features depend on protection of the cryptographic key.

Filtered exports provide integrity protection for the generated bundle but do not provide cryptographic proof of query completeness.

The current authentication configuration is appropriate for prototype demonstration but should be replaced with an enterprise identity solution for production deployment.

---

## 14. Production Improvements

A production implementation should consider stronger identity and authentication controls, TLS, managed signing keys, key rotation, external integrity anchoring, immutable archival storage, least-privilege database identities, monitoring and alerting, backup and recovery validation, idempotency controls, performance testing, and organization-specific retention and privacy policies.

Production requirements should also be reviewed against the applicable regulatory framework before any compliance claim is made.

---

## 15. Conclusion

The completed prototype demonstrates the main engineering properties required from a tamper-evident audit system.

It provides append-oriented event recording, deterministic cryptographic chaining, historical tampering detection, querying, retention and archival, structured redaction, verifiable export, and concurrency-safe event creation.

The implementation has been validated manually and through automated testing.

The project also documents the assumptions, limitations, security boundaries, AI-assisted development decisions, and production improvements required beyond the prototype.

The result is therefore intended as a defensible engineering prototype rather than a claim of production or regulatory certification.