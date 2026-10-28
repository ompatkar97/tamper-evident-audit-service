package com.auditlog.service.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_redaction")
public class RedactionRecord {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "json_paths",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private JsonNode jsonPaths;

    @Column(nullable = false, length = 64)
    private String commitment;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected RedactionRecord() {
    }

    public RedactionRecord(
            UUID id,
            UUID eventId,
            JsonNode jsonPaths,
            String commitment,
            String reason,
            String requestedBy
    ) {
        this.id = id;
        this.eventId = eventId;
        this.jsonPaths = jsonPaths;
        this.commitment = commitment;
        this.reason = reason;
        this.requestedBy = requestedBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public JsonNode getJsonPaths() {
        return jsonPaths;
    }

    public String getCommitment() {
        return commitment;
    }

    public String getReason() {
        return reason;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}