package com.auditlog.service.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_archive_batch")
public class ArchiveBatch {
    @Id
    private UUID id;
    private long firstSequence;
    private long lastSequence;
    private long recordCount;
    private String firstHash;
    private String lastHash;
    private String manifestHash;
    private String requestedBy;
    private String reason;
    private Instant createdAt;

    protected ArchiveBatch() {
    }

    public ArchiveBatch(UUID id, long f, long l, long c, String fh, String lh, String mh, String by, String r) {
        this.id = id;
        firstSequence = f;
        lastSequence = l;
        recordCount = c;
        firstHash = fh;
        lastHash = lh;
        manifestHash = mh;
        requestedBy = by;
        reason = r;
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }
}
