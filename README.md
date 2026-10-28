# Tamper-Evident Audit Log Service

A Java/Spring Boot prototype that provides append-only, tamper-evident audit logging using a SHA-256 hash chain.

The project demonstrates:

- Append-only audit event creation
- Structured JSON payload storage using PostgreSQL JSONB
- Audit querying and pagination
- Cryptographic hash chaining
- Historical tampering detection
- Retention and soft archival
- Structured field-level redaction
- Verifiable bulk export
- Role-based access control
- Automated integrity and concurrency testing
- Regulatory requirement analysis

---

## Technology Stack

- Java 17
- Spring Boot 3.3.x
- Maven
- PostgreSQL
- Spring Data JPA
- Flyway
- Spring Security
- Jackson
- Swagger / OpenAPI
- JUnit 5

---

# Project Structure

```text
tamper-evident-audit-service/
├── pom.xml
├── README.md
├── ATTESTATION.md
├── AI_USAGE.md
├── Dockerfile
├── compose.yaml
├── .env.example
├── requests/
├── docs/
└── src/
    ├── main/
    │   ├── java/com/auditlog/service/
    │   └── resources/
    │       └── db/migration/
    └── test/
        └── java/com/auditlog/service/
```

---

# Prerequisites

For local development:

- Java 17 or newer
- PostgreSQL
- Maven, or IntelliJ IDEA with bundled Maven
- Git

PostgreSQL must be running before starting the application.

---

# Configuration

The application supports environment-based configuration.

Important variables include:

```text
DB_URL
DB_USER
DB_PASSWORD
AUDIT_HMAC_KEY
AUDIT_ADMIN_PASSWORD
AUDIT_WRITER_PASSWORD
AUDIT_READER_PASSWORD
```

Example database URL:

```text
jdbc:postgresql://localhost:5432/postgres
```

Do not commit actual database passwords, HMAC keys, or other secrets to Git.

---

# Database Setup

## Development Database

The application can use a local PostgreSQL database.

Example configuration:

```text
DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USER=postgres
DB_PASSWORD=<your-postgres-password>
AUDIT_HMAC_KEY=<your-hmac-secret>
```

Replace the placeholders with local values.

---

## Test Database

Automated integration tests use a separate PostgreSQL database:

```text
auditdb_test
```

Create it before running integration tests:

```sql
CREATE DATABASE auditdb_test;
```

Using a separate database prevents automated tampering, archival, redaction, and concurrency tests from affecting local development data.

---

# Database Migrations

Flyway manages the database schema.

Migration scripts are located under:

```text
src/main/resources/db/migration/
```

Hibernate is configured primarily for schema validation rather than automatic production schema modification.

---

# Running the Application

## IntelliJ IDEA

Run:

```text
AuditLogApplication
```

from IntelliJ.

## Maven

If Maven is installed:

```bash
mvn spring-boot:run
```

If Maven is not installed globally, the project can be run using IntelliJ's Maven integration.

---

# Swagger / OpenAPI

After starting the service, open:

```text
http://localhost:8080/swagger-ui.html
```

Protected API endpoints use role-based authentication.

Prototype roles include:

```text
AUDIT_WRITER
AUDIT_READER
AUDIT_ADMIN
PRIVACY_ADMIN
AUDITOR
```

The included authentication approach is intended for prototype demonstration only.

---

# Scenario A - Core Audit Log Service

## Create an Audit Event

Endpoint:

```http
POST /api/v1/audit-events
```

Example request:

```json
{
  "eventType": "ACCOUNT_VIEWED",
  "actorId": "EMP101",
  "resourceType": "CLIENT_ACCOUNT",
  "resourceId": "AC0001",
  "payload": {
    "operation": "VIEW",
    "channel": "WEB"
  },
  "timestamp": "2026-10-28T10:00:00Z"
}
```

Every stored event contains integrity metadata including:

```text
sequenceNumber
previousHash
currentHash
```

The first audit event references a configured genesis hash.

---

# Hash Chain

Conceptually:

```text
GENESIS
   |
   v
Event 1
currentHash = H1
   |
   v
Event 2
previousHash = H1
currentHash  = H2
   |
   v
Event 3
previousHash = H2
currentHash  = H3
```

The current hash commits to integrity-protected event data and the previous hash.

JSON payloads are canonicalized before hashing to ensure deterministic hash calculation.

SHA-256 is used for the audit-event hash chain.

---

# Query Audit Events

Endpoint:

```http
GET /api/v1/audit-events
```

Supported filtering includes:

```text
actorId
resourceType
resourceId
eventType
fromTimestamp
toTimestamp
```

Pagination is supported using:

```text
page
size
```

---

# Verify Audit Integrity

Endpoint:

```http
GET /api/v1/audit-events/verify
```

Example valid response:

```json
{
  "valid": true,
  "verifiedRecords": 3,
  "firstInconsistentRecordId": null,
  "firstInconsistentSequence": null,
  "violationType": null,
  "message": "Audit chain is valid"
}
```

Verification checks:

- Sequence continuity
- Previous-hash linkage
- Recalculated current hashes
- Genesis-chain consistency

Verification stops at the first detected inconsistency.

---

# Tampering Demonstration

The prototype has been validated by directly modifying historical PostgreSQL data outside the application.

Example:

```sql
UPDATE audit_event
SET actor_id = 'TAMPERED_USER'
WHERE sequence_number = 2;
```

Running verification afterward produces a failure such as:

```text
CURRENT_HASH_MISMATCH
```

and identifies the first inconsistent sequence.

This demonstrates tamper evidence.

The design does not claim that a basic database-resident hash chain makes database modification impossible.

---

# Scenario B1 - Retention and Archival

Endpoint:

```http
POST /api/v1/audit-events/archive
```

Example:

```json
{
  "cutoff": "2026-10-28T10:02:00Z",
  "requestedBy": "AUDIT_ADMIN",
  "reason": "Retention policy"
}
```

The prototype:

1. Finds eligible historical events.
2. Creates an archive batch.
3. Records archive metadata.
4. Marks eligible events as archived.
5. Preserves the complete underlying chain.

Example response:

```json
{
  "batchId": "<generated-uuid>",
  "archivedRecords": 2
}
```

Legitimate archival does not cause chain verification to fail.

## Retention Limitation

The prototype implements soft archival.

Archived records remain in PostgreSQL so the complete chain can continue to be verified.

A production design requiring physical archival should use independently controlled archival storage such as immutable/WORM storage together with trusted integrity checkpoints.

---

# Scenario B2 - Structured Redaction

Endpoint:

```http
POST /api/v1/audit-events/{eventId}/redactions
```

Example request:

```json
{
  "jsonPaths": [
    "$.accountNumber"
  ],
  "reason": "Privacy requirement",
  "requestedBy": "PRIVACY_ADMIN"
}
```

Suppose the original payload contains:

```json
{
  "operation": "VIEW",
  "channel": "WEB",
  "accountNumber": "123456789",
  "personalIdentifier": "PID-99881"
}
```

After the authorized redaction, normal API responses expose:

```json
{
  "operation": "VIEW",
  "channel": "WEB",
  "accountNumber": "[REDACTED]",
  "personalIdentifier": "PID-99881"
}
```

The original integrity-protected audit event is not rewritten.

Redaction metadata is stored separately, and an HMAC-based commitment provides evidence associated with the redaction operation.

Because the original protected event remains unchanged, audit-chain verification continues to succeed.

## Redaction Limitation

The prototype implements logical/read-time redaction.

The underlying integrity-protected event still contains the original value.

Whether irreversible physical deletion is required depends on the applicable privacy and regulatory requirements.

---

# Scenario B3 - Verifiable Bulk Export

Audit records can be exported by either resource or actor.

## Export by Resource

```http
GET /api/v1/audit-events/export?resourceId=AC0001
```

## Export by Actor

```http
GET /api/v1/audit-events/export?actorId=EMP101
```

Exactly one supported filter must be supplied.

Supplying both or neither results in a validation error.

The export bundle contains information such as:

```text
exportVersion
exportId
generatedAt
filter
records
recordCount
manifestHash
manifestSignature
```

Existing redactions are preserved in exported responses.

---

# Verify an Export

Endpoint:

```http
POST /api/v1/audit-events/export/verify
```

An unchanged exported bundle verifies successfully:

```json
{
  "valid": true,
  "message": "Export bundle is valid"
}
```

If exported data is modified without regenerating valid integrity metadata:

```json
{
  "valid": false,
  "message": "Export bundle integrity check failed"
}
```

This behavior was validated by modifying an exported audit field and submitting the modified bundle to the verification endpoint.

## Export Limitation

The export manifest protects the generated export bundle against undetected modification under the prototype key-trust assumptions.

A filtered export from a basic linear hash chain does not independently provide cryptographic proof that every qualifying source record was included.

Strong cryptographic query-completeness guarantees would require a stronger authenticated data structure or an independently trusted evidence mechanism.

---

# Scenario C - Regulatory Requirement Analysis

Scenario C is documented in:

```text
docs/SCENARIO_C_REQUIREMENTS.md
```

The analysis begins from the ambiguous requirement:

```text
Regulators need to be able to audit access to client account data.
```

The document covers:

- Identified ambiguities
- Stakeholder questions
- Explicit prototype assumptions
- Normalized requirements
- Acceptance criteria
- Technical design mapping
- Implemented scope
- Out-of-scope items
- Risks and limitations

Prototype assumptions are documented as engineering assumptions rather than regulatory facts.

---

# Security Model

The prototype uses role-based authorization.

Roles include:

```text
AUDIT_WRITER
AUDIT_READER
AUDIT_ADMIN
PRIVACY_ADMIN
AUDITOR
```

Responsibilities are separated so that different operations can require different roles.

For production deployment, stronger controls should include:

- Enterprise identity using OIDC/OAuth2
- TLS
- Managed secret storage
- Least-privilege PostgreSQL accounts
- Centralized key management
- Key rotation
- External integrity anchoring where required
- Security monitoring and alerting

---

# Automated Testing

Integration tests use:

```text
auditdb_test
```

rather than the normal development database.

The automated test suite covers critical integrity behavior including:

- JSON canonicalization
- Cryptographic hash generation
- Valid hash-chain creation
- Sequence continuity
- Previous-hash linkage
- Historical database tampering detection
- Retention / archival
- Verification after archival
- Structured redaction
- Verification after redaction
- Export generation
- Export verification
- Modified-export detection
- Concurrent audit writes
- Prevention of duplicate chain sequence numbers
- Final chain verification after concurrent writes

Run:

```bash
mvn clean verify
```

Or use:

```text
IntelliJ IDEA
→ Maven
→ Lifecycle
→ clean
→ verify
```

A successful verification run should finish with:

```text
BUILD SUCCESS
```

---

# Concurrency Control

Audit-event creation must preserve a single ordered chain even when multiple requests arrive concurrently.

The prototype serializes updates to the chain state using transactional database locking.

Automated concurrent-write testing verifies that:

- sequence numbers remain unique
- expected sequence numbers are present
- previous-hash relationships remain correct
- the final audit chain verifies successfully

This favors integrity and simple reasoning over maximum write throughput.

A higher-throughput production architecture could consider partitioned chains or another integrity model depending on ordering requirements.

---

# Threat Model and Important Limitations

## Database Administrator Threat

The hash chain detects modification when integrity-protected event data changes without corresponding valid chain reconstruction.

However, a sufficiently privileged attacker who can rewrite all historical records and recompute all subsequent hashes within the same trust boundary is outside the guarantees of a plain database-resident hash chain.

Production strengthening options include:

```text
externally signed checkpoints
managed signing keys
HSM/KMS-backed signatures
independently controlled immutable storage
external integrity anchoring
```

## HMAC Key

The prototype uses HMAC for integrity-related evidence such as redaction/export operations.

Security depends on keeping the HMAC key secret.

Production deployments should use managed secret/key infrastructure rather than application configuration files.

## Authentication

The current authentication configuration is suitable for prototype demonstration.

It should not be considered a production identity solution.

---

# Test and Build Notes

The project targets:

```text
Java 17
```

The code may run under a newer compatible JDK while still compiling with Java 17 release compatibility.

The final project should be validated using:

```bash
mvn clean verify
```

before submission.

---

# API Examples

Additional HTTP request examples are available under:

```text
requests/audit-service.http
```

---

# Architecture and Design Documentation

See:

```text
docs/ARCHITECTURE.md
docs/HASH_CHAIN_DESIGN.md
docs/DATABASE_DESIGN.md
docs/RETENTION_AND_ARCHIVAL.md
docs/REDACTION_DESIGN.md
docs/EXPORT_DESIGN.md
docs/SCENARIO_C_REQUIREMENTS.md
docs/SECURITY.md
docs/THREAT_MODEL.md
docs/TRADE_OFFS_AND_LIMITATIONS.md
docs/LIVE_DEFENSE_GUIDE.md
docs/PRODUCTION_READINESS.md
```

These documents describe the architecture, integrity model, security assumptions, design trade-offs, and production-readiness considerations.

---

# AI-Assisted Development

AI tools were used as development assistants during implementation, debugging, testing, and documentation.

Detailed AI usage and engineering decisions are recorded in:

```text
AI_USAGE.md
```

The AI usage log should distinguish suggestions that were:

```text
Accepted
Modified
Rejected
```

and record how important generated changes were validated.

The candidate remains responsible for the correctness, security, maintainability, and engineering decisions in this repository.

---

# Attestation

Candidate attestation is maintained in:

```text
ATTESTATION.md
```

The file should contain the required candidate details and the exact attestation statement required by the assignment.

---

# Production Improvements

Before production deployment, the following areas require additional engineering:

- Enterprise authentication and authorization
- TLS and service-to-service authentication
- Managed cryptographic keys
- Key rotation
- External integrity checkpoints
- Independent immutable archival storage
- Database least privilege
- Operational monitoring and alerts
- Backup and disaster-recovery validation
- Rate limiting
- Idempotency strategy
- Performance/load testing
- Privacy review of permitted audit payload data
- Regulatory/jurisdiction-specific retention rules

---

# Summary

This prototype demonstrates how an audit-log service can provide:

```text
Append-only event recording
        +
SHA-256 hash chaining
        +
Tampering detection
        +
Retention / archival
        +
Structured redaction
        +
Verifiable export
        +
Automated integrity testing
```

The implementation intentionally distinguishes prototype guarantees from stronger production and regulatory guarantees.