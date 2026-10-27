# Scenario C - Requirement Analysis
## Original requirement
"Regulators need to be able to audit access to client account data."

## Ambiguities
Access could mean view, search, download, update or failed attempts. Actor types, client-data scope, jurisdiction, retention, regulator delivery method, latency, completeness evidence, redaction and authorization are unspecified.

## Stakeholder questions
Compliance: which regulation, event scope, retention and evidence are mandatory? Security: which actors/service accounts and failed attempts are recorded? Privacy/legal: which fields may be logged or exported? Product/data owners: which resources represent client-account data? Operations: required availability, throughput, recovery, monitoring and archive location?

## Prototype assumptions
Access includes ACCOUNT_VIEWED, ACCOUNT_SEARCHED, ACCOUNT_DOWNLOADED, ACCOUNT_UPDATED and ACCOUNT_ACCESS_DENIED. Actors are authenticated workforce or service identities. Regulators receive a verifiable export rather than direct database access. Events store identifiers and metadata, not unnecessary secrets. UTC is used. These are prototype assumptions, not statements of regulatory compliance.

## Normalized requirements
- C-01 Record every successful or denied in-scope account access with actor, operation, account resource, result, timestamp and correlation metadata. Acceptance: create API persists and chains an event.
- C-02 Authorized readers can filter by actor, resource, event type and time. Acceptance: query endpoint and tests.
- C-03 Historical modification is detectable. Acceptance: verification reports first inconsistency.
- C-04 Authorized auditors can export integrity-protected results. Acceptance: signed export and verification endpoint.
- C-05 Sensitive values are minimized and redactable in the read projection with append-only evidence. Acceptance: redaction endpoint records paths and HMAC commitment.

## Out of scope
Real regulator federation/portal, real banking integration, named-law certification, HSM/key rotation, independently operated WORM archive and cryptographic query-completeness proof. Production delivery must define and implement these based on stakeholder answers.
