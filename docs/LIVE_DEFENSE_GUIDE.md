# Live defense guide
1. Start Compose and open Swagger. 2. Create three events. 3. Query and verify. 4. Modify payload directly in PostgreSQL and show CURRENT_HASH_MISMATCH. 5. Recreate data. 6. Archive using cutoff and verify. 7. Record redaction evidence. 8. Export and verify; modify bundle and show failure.

Be ready to explain canonicalization, length-prefixing, pessimistic chain lock, same-database trust limitation, HMAC key protection, archive behavior, filtered-export completeness limitation, and Scenario C assumptions.
