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
@Table(name = "audit_event")
public class AuditEvent {

 @Id
 private UUID id;

 @Column(nullable = false, unique = true)
 private long sequenceNumber;

 @Column(nullable = false)
 private String eventType;

 @Column(nullable = false)
 private String actorId;

 @Column(nullable = false)
 private String resourceType;

 @Column(nullable = false)
 private String resourceId;

 @JdbcTypeCode(SqlTypes.JSON)
 @Column(nullable = false, columnDefinition = "jsonb")
 private JsonNode payload;

 @Column(nullable = false)
 private Instant eventTimestamp;

 @Column(nullable = false, length = 64)
 private String previousHash;

 @Column(nullable = false, length = 64)
 private String currentHash;

 @Column(nullable = false)
 private Instant createdAt;

 @Column(nullable = false)
 private boolean archived;

 private UUID archiveBatchId;

 protected AuditEvent() {
 }

 public AuditEvent(
         UUID id,
         long sequenceNumber,
         String eventType,
         String actorId,
         String resourceType,
         String resourceId,
         JsonNode payload,
         Instant eventTimestamp,
         String previousHash,
         String currentHash
 ) {
  this.id = id;
  this.sequenceNumber = sequenceNumber;
  this.eventType = eventType;
  this.actorId = actorId;
  this.resourceType = resourceType;
  this.resourceId = resourceId;
  this.payload = payload;
  this.eventTimestamp = eventTimestamp;
  this.previousHash = previousHash;
  this.currentHash = currentHash;
  this.createdAt = Instant.now();
  this.archived = false;
 }

 public UUID getId() {
  return id;
 }

 public long getSequenceNumber() {
  return sequenceNumber;
 }

 public String getEventType() {
  return eventType;
 }

 public String getActorId() {
  return actorId;
 }

 public String getResourceType() {
  return resourceType;
 }

 public String getResourceId() {
  return resourceId;
 }

 public JsonNode getPayload() {
  return payload;
 }

 public Instant getEventTimestamp() {
  return eventTimestamp;
 }

 public String getPreviousHash() {
  return previousHash;
 }

 public String getCurrentHash() {
  return currentHash;
 }

 public Instant getCreatedAt() {
  return createdAt;
 }

 public boolean isArchived() {
  return archived;
 }

 public UUID getArchiveBatchId() {
  return archiveBatchId;
 }

 public void archive(UUID archiveBatchId) {
  this.archived = true;
  this.archiveBatchId = archiveBatchId;
 }
}