package com.auditlog.service.repository;

import com.auditlog.service.entity.AuditEvent;

import java.time.Instant;
import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.domain.Specification;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {
    List<AuditEvent> findAllByOrderBySequenceNumberAsc();

    List<AuditEvent> findByEventTimestampBeforeAndArchivedFalseOrderBySequenceNumberAsc(Instant cutoff);

    List<AuditEvent> findByActorIdOrderBySequenceNumberAsc(String actorId);

    List<AuditEvent> findByResourceIdOrderBySequenceNumberAsc(String resourceId);
}
