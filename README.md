# Tamper-Evident Audit Log Service
Java 17 / Spring Boot / PostgreSQL prototype implementing Scenario A core append-only hash chaining, Scenario B retention, redaction evidence and signed export, and Scenario C compliance analysis.

## Run
1. Copy `.env.example` to `.env` and replace secrets.
2. `docker compose up --build`
3. Swagger: `http://localhost:8080/swagger-ui.html`
4. Examples: `requests/audit-service.http`

## Validate
`mvn clean verify` requires Docker because the integration test uses Testcontainers.

## Integrity model
Every event commits to version, sequence, event UUID, event fields, canonical JSON payload, event timestamp and previous hash using SHA-256 with length-prefixed fields. A pessimistically locked `chain_state` row serializes appends and prevents chain forks.

## Important limitations
A hash chain in the same database detects ordinary changes but does not defeat an administrator able to rewrite both records and hashes. Production designs should sign/checkpoint externally and use WORM storage or an independently controlled trust service. Redaction in this prototype records an append-only HMAC commitment but does not mutate the immutable event; production read projections must apply masking. Archive is soft archival in the same database. Filtered exports prove bundle integrity/authenticity under the HMAC key, not cryptographic completeness of the original query result.

## Prototype users
Passwords default only for local demonstration and must be replaced through environment variables: writer, reader, admin.
