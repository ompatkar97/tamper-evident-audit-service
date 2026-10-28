# Architecture - Tamper-Evident Audit Log Service

## 1. Purpose

Briefly describe the system:

- Append-only audit-event storage
- Tamper-evident hash chaining
- Query and verification
- Retention / archival
- Structured redaction
- Verifiable export

---

## 2. Technology Stack

- Java 17
- Spring Boot 3.x
- PostgreSQL
- Spring Data JPA
- Flyway
- Spring Security
- SHA-256
- HMAC-SHA256
- JUnit 5

---

## 3. High-Level Architecture

```text
Client / Swagger
       |
       v
AuditController
       |
       +-----------------------------+
       |                             |
       v                             v
AuditService                  ExtensionService
       |                       |      |      |
       |                    Archive Redact Export
       |                             |
       v                             v
CryptoService             RedactionProjectionService
       |                             |
       +-------------+---------------+
                     |
                     v
                 PostgreSQL