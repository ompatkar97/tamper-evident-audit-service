package com.auditlog.service.repository;

import com.auditlog.service.entity.RedactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RedactionRepository
        extends JpaRepository<RedactionRecord, UUID> {

    List<RedactionRecord> findByEventIdOrderByCreatedAtAsc(
            UUID eventId
    );
}