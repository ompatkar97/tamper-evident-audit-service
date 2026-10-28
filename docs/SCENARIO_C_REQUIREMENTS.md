# Scenario C - Regulatory Audit Requirement Analysis

## 1. Original Requirement

"Regulators need to be able to audit access to client account data."

The requirement is intentionally ambiguous and is not sufficiently specific for direct implementation.

Before implementation, the requirement needs to be clarified in terms of what constitutes access, which actors and resources are in scope, which audit information is required, retention expectations, privacy requirements, and how regulators receive the audit information.


## 2. Identified Ambiguities

### 2.1 What constitutes "access"?

The requirement does not define which operations must be audited.

Possible operations include:

- Viewing an account
- Searching for an account
- Downloading account information
- Updating account information
- Failed or denied access attempts
- API-based access
- Administrative access


### 2.2 Who is considered an actor?

Possible actors may include:

- Employees
- Administrators
- Service accounts
- Internal applications
- External systems

The requirement does not specify which actor categories must be captured.


### 2.3 What is "client account data"?

The scope of client account data is not explicitly defined.

Possible examples include:

- Account identifiers
- Account details
- Transactions
- Statements
- Customer identifiers
- Personal information

Sensitive information should not automatically be copied into audit payloads unless required.


### 2.4 What information must an audit event contain?

The requirement does not specify the evidence regulators need.

Potential audit information includes:

- Actor identity
- Operation
- Resource type
- Resource identifier
- Timestamp
- Result of the operation
- Correlation/request identifier
- Relevant non-sensitive context


### 2.5 How will regulators access the audit information?

Possible approaches include:

- Direct application access
- Read-only reporting interface
- Search/query API
- Exported evidence package

The original requirement does not specify the delivery mechanism.


### 2.6 What retention period applies?

The requirement does not define:

- How long records must remain active
- When records may be archived
- How archived records should be retrieved
- Whether archived records must remain independently verifiable


### 2.7 What privacy rules apply?

Audit payloads may themselves contain sensitive information.

Questions include:

- Which sensitive fields are permitted in audit records?
- Which fields must be masked?
- Can information later be redacted?
- Who is authorized to perform redaction?
- Must evidence of a redaction be retained?


### 2.8 What integrity guarantees are required?

"Audit" does not automatically define the required integrity model.

Questions include:

- Is detection of historical modification required?
- Must deletion of records be detectable?
- Does verification need an externally trusted checkpoint?
- Must exported records be independently verifiable?


### 2.9 Which regulation or jurisdiction applies?

No particular law, regulation, jurisdiction, or retention schedule is identified.

Therefore this prototype does not claim compliance with a specific regulatory framework.


## 3. Stakeholder Questions

### Compliance

1. Which regulatory framework applies?
2. What activities qualify as auditable account access?
3. Are unsuccessful or denied attempts required?
4. What retention period must be enforced?
5. What evidence must be available during an audit?
6. Does an auditor require proof of completeness as well as integrity?


### Security

1. Which actor types must be recorded?
2. How should service accounts be identified?
3. How should privileged administrative access be represented?
4. Who may create, read, verify, archive, redact, and export audit information?
5. Where should cryptographic keys be managed in production?


### Privacy / Legal

1. Which client fields may appear in audit payloads?
2. Which values must never be logged?
3. When can a field be redacted?
4. Is logical masking sufficient or is irreversible deletion required?
5. Who may authorize redaction?


### Product / Data Owners

1. Which resource types represent client account data?
2. Which business operations correspond to access?
3. Which applications must produce audit events?
4. Which identifiers can safely appear in audit records?


### Operations

1. What audit-event throughput is expected?
2. What availability is required?
3. What should happen if audit persistence fails?
4. How should chain verification failures be alerted?
5. Where should archived records be stored?


## 4. Prototype Assumptions

Because the original requirement does not answer the questions above, the prototype uses the following explicit engineering assumptions.

These assumptions are NOT claimed to be regulatory facts.

### A-01 - Access Operations

For this prototype, account access can be represented using event types such as:

- ACCOUNT_VIEWED
- ACCOUNT_SEARCHED
- ACCOUNT_DOWNLOADED
- ACCOUNT_UPDATED
- ACCOUNT_ACCESS_DENIED


### A-02 - Actor Identification

Every audit event contains an `actorId`.

The prototype assumes that the calling system has already established the actor's identity.


### A-03 - Resource Identification

Client accounts are represented using:

resourceType = CLIENT_ACCOUNT

and an application-specific `resourceId`.


### A-04 - Audit Event Evidence

Each event contains at least:

- eventType
- actorId
- resourceType
- resourceId
- payload
- event timestamp
- sequence number
- previous hash
- current hash


### A-05 - Audit Integrity

Historical modification should be detectable.

The prototype therefore uses an append-oriented SHA-256 hash chain.


### A-06 - Regulatory Delivery

The prototype assumes that regulators or authorized auditors receive filtered, verifiable exports rather than direct PostgreSQL database access.


### A-07 - Sensitive Data

Audit payloads should minimize sensitive information.

Where structured sensitive information is already present, authorized redaction can mask configured fields in read/export projections while retaining integrity evidence.


### A-08 - Retention

Older records may be archived.

The prototype implements soft archival in the primary database while retaining the records required for full-chain verification.


### A-09 - Time

Audit event timestamps use UTC.


## 5. Normalized Requirements

### C-01 - Record Auditable Access

The system shall accept audit events representing access to client account resources.

Acceptance criteria:

- An authorized audit writer can submit an event.
- The event identifies the actor.
- The event identifies the resource.
- The event identifies the operation through its event type.
- The event contains a timestamp.

Implementation:

POST /api/v1/audit-events


### C-02 - Preserve Historical Integrity

Audit records shall be cryptographically linked so unauthorized modification of protected historical event data can be detected.

Acceptance criteria:

- Each event has a sequence number.
- Each event references its predecessor's hash.
- Recalculation validates unchanged events.
- Direct modification of protected event data produces a verification failure.

Implementation:

SHA-256 chain and:

GET /api/v1/audit-events/verify

Validated behavior:

Direct modification of sequence 2 was detected as:

CURRENT_HASH_MISMATCH


### C-03 - Query Audit Evidence

Authorized users shall be able to query audit records using relevant audit dimensions.

Supported dimensions include:

- actorId
- resourceType
- resourceId
- eventType
- timestamp range

The result shall support pagination.

Implementation:

GET /api/v1/audit-events


### C-04 - Retain and Archive Records

The system shall support legitimate archival of eligible audit records without interpreting the archival operation as unauthorized tampering.

Implementation:

POST /api/v1/audit-events/archive

Prototype behavior:

- Eligible records are marked archived.
- An archive batch is created.
- Archived records remain in the primary database.
- Full chain verification continues to succeed.


### C-05 - Protect Sensitive Audit Payload Fields

Authorized privacy operations shall be able to redact selected structured payload fields without rewriting the original integrity-protected audit event.

Implementation:

POST /api/v1/audit-events/{eventId}/redactions

Validated prototype example:

Original read representation:

accountNumber = 123456789

After authorized redaction:

accountNumber = [REDACTED]

The original audit event remains available for integrity verification and the audit chain remains valid.


### C-06 - Export Audit Evidence

Authorized auditors shall be able to export audit records for an actor or resource.

Implementation examples:

GET /api/v1/audit-events/export?actorId=EMP101

GET /api/v1/audit-events/export?resourceId=AC0001


### C-07 - Verify Export Integrity

An export bundle shall contain integrity metadata sufficient to detect modification of the generated export bundle.

Prototype export includes:

- export version
- export identifier
- generation timestamp
- filter criteria
- exported audit records
- record count
- manifest hash
- manifest signature

Implementation:

POST /api/v1/audit-events/export/verify

Validated behavior:

Unmodified bundle -> valid

Modified exported event -> invalid


### C-08 - Preserve Redaction During Export

An export shall not bypass an existing read-time redaction.

Validated example:

AC0099 contains:

accountNumber = [REDACTED]

in the generated export rather than exposing the original account number.


## 6. Security Model

The prototype separates responsibilities using application roles.

Examples include:

- AUDIT_WRITER
- AUDIT_READER
- AUDIT_ADMIN
- PRIVACY_ADMIN
- AUDITOR

Examples of intended separation:

- Writers create audit events.
- Readers query audit events.
- Administrators perform administrative audit operations.
- Privacy administrators request redaction.
- Auditors verify and export audit evidence.

The prototype currently uses simple application authentication for demonstration purposes.

Production deployment should use the organization's identity platform and enforce least-privilege authorization.


## 7. Tamper-Evidence Design

Audit events form a sequential hash chain.

Conceptually:

GENESIS
|
v
Event 1
H1
|
v
Event 2
previousHash = H1
currentHash = H2
|
v
Event 3
previousHash = H2
currentHash = H3

Verification recalculates each event hash and validates the chain relationship.

The prototype successfully detected direct modification of historical event data.

A limitation is that a hash chain stored entirely within the same administrative/database trust boundary does not prevent a sufficiently privileged attacker from rewriting both data and hashes.

A stronger production design could introduce externally signed checkpoints, independently controlled immutable storage, or an external trust anchor.


## 8. Retention Design

The prototype uses soft archival.

An archive operation:

1. Identifies eligible records using a cutoff.
2. Creates an archive batch.
3. Records chain-related metadata for the archived range.
4. Marks eligible events as archived.
5. Leaves historical records available to the verifier.

This design was chosen for the prototype so archival semantics can be demonstrated without breaking full-chain verification.

It is not equivalent to moving records to independent cold or immutable storage.


## 9. Redaction Design

The prototype does not rewrite the immutable audit event when a privacy redaction is requested.

Instead:

Original immutable event
|
+----> integrity verification
|
+----> redaction metadata
|
v
read projection
|
v
[REDACTED]

This allows the audit chain to remain valid while preventing the selected field from appearing in normal read/export responses.

The prototype stores cryptographic redaction evidence using an HMAC-based commitment.

### Limitation

This is logical redaction rather than irreversible physical deletion.

The original sensitive value remains within the underlying integrity-protected audit record.

Whether that is acceptable depends on the clarified legal/privacy requirement and storage architecture.


## 10. Export Design

Exports may be requested by actorId or resourceId.

The generated bundle includes records plus a manifest hash and HMAC-based signature.

Verification recomputes the manifest information and checks the supplied integrity evidence.

The prototype demonstrated:

- valid unmodified export
- detection of modified export data
- preservation of redacted fields during export


## 11. Export Completeness Limitation

A filtered export from a basic linear hash chain does not, by itself, cryptographically prove that every qualifying source record was included.

For example:

Full chain:

1 -> 2 -> 3 -> 4 -> 5

Filtered export:

1, 3, 5

The integrity mechanism can protect the generated bundle against later modification, but the prototype does not claim cryptographic proof that an exporter could not have omitted another qualifying source record.

A production requirement for cryptographic query completeness would require a stronger authenticated data structure or trusted external evidence mechanism.


## 12. Explicitly Out of Scope

The prototype intentionally does not implement:

- Real regulator identity federation
- Regulator-facing user interface
- Integration with a production banking/account platform
- Certification against a named regulatory framework
- Hardware Security Module integration
- Production key rotation
- Independently controlled WORM archival storage
- Cryptographic proof of filtered-query completeness
- Production-scale performance and disaster-recovery architecture

These items require organization-specific requirements and infrastructure decisions.


## 13. Implementation Traceability

Scenario A provides:

- append-oriented audit recording
- search/filtering
- pagination
- cryptographic chaining
- chain verification
- tamper detection

Scenario B provides:

- retention / soft archival
- structured redaction
- verifiable export bundles

Scenario C maps the original ambiguous compliance statement onto these implemented technical capabilities while explicitly identifying assumptions and remaining gaps.


## 14. Final Clarified Prototype Requirement

For this prototype, the original statement is normalized as:

"The system shall create integrity-protected audit records for defined client-account access operations. Each audit record shall identify the actor, operation, affected client-account resource and timestamp. Authorized users shall be able to query the records, authorized privacy personnel shall be able to request structured redaction, administrators shall be able to archive eligible records, and authorized auditors shall be able to export and verify audit evidence. The prototype shall detect unauthorized modification of protected historical event data."

This normalized requirement is a prototype engineering interpretation of the original ambiguous statement and is not represented as an authoritative regulatory requirement.git