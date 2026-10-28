package com.auditlog.service.service;

import com.auditlog.service.config.AuditProperties;
import com.auditlog.service.crypto.CryptoService;
import com.auditlog.service.dto.ApiDtos.AuditEventResponse;
import com.auditlog.service.dto.ApiDtos.CreateAuditEventRequest;
import com.auditlog.service.dto.ApiDtos.VerifyResponse;
import com.auditlog.service.entity.AuditEvent;
import com.auditlog.service.entity.ChainState;
import com.auditlog.service.repository.AuditEventRepository;
import com.auditlog.service.repository.ChainStateRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

 private static final int MAX_PAGE_SIZE = 100;

 private final AuditEventRepository events;
 private final ChainStateRepository states;
 private final CryptoService crypto;
 private final AuditProperties properties;

 public AuditService(
         AuditEventRepository events,
         ChainStateRepository states,
         CryptoService crypto,
         AuditProperties properties
 ) {
  this.events = events;
  this.states = states;
  this.crypto = crypto;
  this.properties = properties;
 }

 @Transactional
 public AuditEventResponse create(CreateAuditEventRequest request) {

  ChainState state = states.lock();

  long sequenceNumber = state.getLastSequence() + 1;
  String previousHash = state.getLastHash();

  UUID eventId = UUID.randomUUID();

  /*
   * Canonical JSON is used ONLY for calculating the hash.
   * The actual JsonNode is stored in PostgreSQL JSONB.
   */
  String canonicalPayload =
          crypto.canonical(request.payload());

  String currentHash = crypto.eventHash(
          sequenceNumber,
          eventId,
          request.eventType(),
          request.actorId(),
          request.resourceType(),
          request.resourceId(),
          canonicalPayload,
          request.timestamp(),
          previousHash
  );

  AuditEvent auditEvent = new AuditEvent(
          eventId,
          sequenceNumber,
          request.eventType(),
          request.actorId(),
          request.resourceType(),
          request.resourceId(),
          request.payload(),
          request.timestamp(),
          previousHash,
          currentHash
  );

  AuditEvent savedEvent = events.save(auditEvent);

  state.advance(
          sequenceNumber,
          currentHash
  );

  return map(savedEvent);
 }

 @Transactional(readOnly = true)
 public Page<AuditEventResponse> query(
         String actorId,
         String resourceType,
         String resourceId,
         String eventType,
         Instant fromTimestamp,
         Instant toTimestamp,
         int page,
         int size
 ) {

  Specification<AuditEvent> specification =
          (root, query, criteriaBuilder) ->
                  criteriaBuilder.conjunction();

  if (actorId != null && !actorId.isBlank()) {

   specification = specification.and(
           (root, query, cb) ->
                   cb.equal(
                           root.get("actorId"),
                           actorId
                   )
   );
  }

  if (resourceType != null && !resourceType.isBlank()) {

   specification = specification.and(
           (root, query, cb) ->
                   cb.equal(
                           root.get("resourceType"),
                           resourceType
                   )
   );
  }

  if (resourceId != null && !resourceId.isBlank()) {

   specification = specification.and(
           (root, query, cb) ->
                   cb.equal(
                           root.get("resourceId"),
                           resourceId
                   )
   );
  }

  if (eventType != null && !eventType.isBlank()) {

   specification = specification.and(
           (root, query, cb) ->
                   cb.equal(
                           root.get("eventType"),
                           eventType
                   )
   );
  }

  if (fromTimestamp != null) {

   specification = specification.and(
           (root, query, cb) ->
                   cb.greaterThanOrEqualTo(
                           root.get("eventTimestamp"),
                           fromTimestamp
                   )
   );
  }

  if (toTimestamp != null) {

   specification = specification.and(
           (root, query, cb) ->
                   cb.lessThanOrEqualTo(
                           root.get("eventTimestamp"),
                           toTimestamp
                   )
   );
  }

  int pageSize =
          Math.min(
                  Math.max(size, 1),
                  MAX_PAGE_SIZE
          );

  int pageNumber = Math.max(page, 0);

  PageRequest pageRequest = PageRequest.of(
          pageNumber,
          pageSize,
          Sort.by(
                  Sort.Direction.ASC,
                  "sequenceNumber"
          )
  );

  return events
          .findAll(specification, pageRequest)
          .map(this::map);
 }

 @Transactional(readOnly = true)
 public VerifyResponse verify() {

  List<AuditEvent> auditEvents =
          events.findAllByOrderBySequenceNumberAsc();

  String expectedPreviousHash =
          properties.genesisHash();

  long expectedSequence = 1;
  long verifiedRecords = 0;

  for (AuditEvent event : auditEvents) {

   /*
    * Check sequence integrity.
    */
   if (event.getSequenceNumber()
           != expectedSequence) {

    return invalid(
            verifiedRecords,
            event,
            "SEQUENCE_GAP"
    );
   }

   /*
    * Check that this event references
    * the previous event correctly.
    */
   if (!event.getPreviousHash()
           .equals(expectedPreviousHash)) {

    return invalid(
            verifiedRecords,
            event,
            "PREVIOUS_HASH_MISMATCH"
    );
   }

   /*
    * Recalculate hash from the database record.
    *
    * CryptoService.recalc() must canonicalize
    * event.getPayload() before hashing.
    */
   String recalculatedHash =
           crypto.recalc(event);

   if (!event.getCurrentHash()
           .equals(recalculatedHash)) {

    return invalid(
            verifiedRecords,
            event,
            "CURRENT_HASH_MISMATCH"
    );
   }

   expectedPreviousHash =
           event.getCurrentHash();

   expectedSequence++;

   verifiedRecords++;
  }

  return new VerifyResponse(
          true,
          verifiedRecords,
          null,
          null,
          null,
          "Audit chain is valid"
  );
 }

 private VerifyResponse invalid(
         long verifiedRecords,
         AuditEvent event,
         String violationType
 ) {

  return new VerifyResponse(
          false,
          verifiedRecords,
          event.getId(),
          event.getSequenceNumber(),
          violationType,
          "First chain inconsistency detected"
  );
 }

 public AuditEventResponse map(
         AuditEvent event
 ) {

  return new AuditEventResponse(
          event.getId(),
          event.getSequenceNumber(),
          event.getEventType(),
          event.getActorId(),
          event.getResourceType(),
          event.getResourceId(),

          // Already JsonNode
          event.getPayload(),

          event.getEventTimestamp(),
          event.getPreviousHash(),
          event.getCurrentHash(),
          event.isArchived()
  );
 }

 @Transactional(readOnly = true)
 public AuditEvent require(UUID id) {

  return events
          .findById(id)
          .orElseThrow(
                  () -> new NoSuchElementException(
                          "Audit event not found: " + id
                  )
          );
 }
}