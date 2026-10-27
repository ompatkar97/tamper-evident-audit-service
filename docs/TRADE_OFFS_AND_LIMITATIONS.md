# Trade-offs and limitations
- Single chain-state lock favors correctness over write throughput. Partitioned chains could scale by tenant/resource but complicate ordering and verification.
- Database-resident SHA-256 chains are tamper-evident only while trusted checkpoints/keys remain outside attacker control.
- Soft archival demonstrates retention semantics, not cold-storage deletion.
- Redaction evidence exists, but masking must be enforced in production read projections.
- HMAC exports prove integrity/authenticity, not query completeness.
- HTTP Basic is demonstration-only. Production requires enterprise identity, TLS, least privilege and key vault/HSM.
