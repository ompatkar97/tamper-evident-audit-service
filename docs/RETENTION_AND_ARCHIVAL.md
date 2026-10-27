# Retention and archival
The prototype marks eligible records archived and creates a manifest batch with sequence boundaries and hashes. Records remain available for full-chain verification, avoiding false positives. Production should copy to independently controlled immutable storage and validate signed checkpoints before removal from active storage.
