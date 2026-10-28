# Trade-offs and Limitations

## 1. Testing Approach

The project uses both manual API validation and automated tests.

### Automated Tests

Critical automated coverage includes:

- Deterministic JSON canonicalization
- SHA-256 hashing
- Valid audit-chain construction
- Sequence continuity
- Previous-hash linkage
- Direct database tampering detection
- Retention and archival
- Chain verification after archival
- Structured redaction
- Chain verification after redaction
- Export generation and verification
- Modified-export detection
- Concurrent audit writes

Integration tests run against a dedicated PostgreSQL database:

auditdb_test

This is kept separate from the development database because some tests deliberately modify audit records.

The project is validated using:

mvn clean verify

Expected result:

BUILD SUCCESS

---

## 2. Hash-Chain Trade-off

The prototype uses a sequential SHA-256 hash chain.

Advantages:

- Simple to implement and explain
- Historical modification can be detected
- Each record is linked to its predecessor
- Verification can identify the first inconsistent record

Limitation:

A sufficiently privileged attacker who can modify all historical records and recompute all subsequent hashes may reconstruct a consistent chain.

Production strengthening could include externally signed checkpoints, KMS/HSM-backed signing, or independent immutable storage.

---

## 3. Concurrency Trade-off

A shared chain-state row is transactionally locked when appending events.

Advantages:

- Prevents duplicate sequence numbers
- Prevents chain forks
- Provides deterministic global ordering

Limitation:

The shared lock serializes the critical section of audit writes and may limit throughput under high write volume.

A production system with very high throughput could use partitioned chains if global ordering is not required.

---

## 4. Retention Trade-off

The prototype implements soft archival.

Archived events remain in PostgreSQL and are marked as archived.

Advantages:

- Simple prototype implementation
- Full audit chain remains available
- Archival does not cause false tampering alerts

Limitation:

This does not provide independent cold-storage or WORM guarantees.

A production implementation may move archived records to immutable storage while retaining trusted chain checkpoints.

---

## 5. Redaction Trade-off

The prototype uses logical/read-time structured redaction.

The original integrity-protected event remains unchanged while selected fields are masked in read/export projections.

Advantages:

- Hash-chain integrity remains valid
- Redaction history is separately recorded
- Individual structured fields can be masked

Limitation:

The original sensitive value still exists in underlying audit storage.

This approach may not satisfy a requirement for irreversible physical erasure.

The applicable legal/privacy requirement must determine the production strategy.

---

## 6. HMAC Key Management

HMAC is used for integrity-related evidence such as redaction commitments and export signatures.

Limitation:

Security depends on protecting the HMAC key.

The prototype configuration is not a production key-management solution.

Production deployment should use managed secret/key infrastructure with appropriate access control and rotation.

---

## 7. Export Trade-off

The export bundle contains records, manifest integrity information, and an HMAC signature.

The implementation detects modification of an already-generated bundle.

Limitation:

For a filtered export from a simple linear chain, the prototype does not cryptographically prove that every qualifying database record was included.

Strong query-completeness requirements would require an authenticated indexing/data structure or independently trusted evidence mechanism.

---

## 8. Authentication

The prototype uses simple role-based authentication suitable for demonstrating authorization boundaries.

Limitation:

It is not intended as a production identity architecture.

Production deployment should integrate enterprise identity using technologies such as OIDC/OAuth2 and enforce least privilege.

---

## 9. Database Trust Boundary

PostgreSQL data is treated as potentially modifiable for tamper-detection testing.

However, hashes and audit records reside within the same broad application/database trust architecture.

Production environments requiring stronger guarantees should introduce an external trust anchor.

---

## 10. Testing Limitations

The automated suite focuses on critical integrity and business behavior.

The prototype does not attempt exhaustive testing for:

- Production-scale load
- Multi-region behavior
- Disaster recovery
- KMS/HSM failures
- Long-running retention jobs
- Large export volumes
- Regulatory certification
- External identity-provider failures

These areas would require environment-specific production requirements and infrastructure.