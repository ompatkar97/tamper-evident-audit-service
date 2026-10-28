package com.auditlog.service.controller;

import com.auditlog.service.dto.ApiDtos.*;
import com.auditlog.service.service.*;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-events")
public class AuditController {
    private final AuditService audit;
    private final ExtensionService ext;

    public AuditController(AuditService a, ExtensionService e) {
        audit = a;
        ext = e;
    }

    @PostMapping
    public ResponseEntity<AuditEventResponse> create(@Valid @RequestBody CreateAuditEventRequest r) {
        return ResponseEntity.status(201).body(audit.create(r));
    }

    @GetMapping
    public Page<AuditEventResponse> query(@RequestParam(required = false) String actorId, @RequestParam(required = false) String resourceType, @RequestParam(required = false) String resourceId, @RequestParam(required = false) String eventType, @RequestParam(required = false) Instant fromTimestamp, @RequestParam(required = false) Instant toTimestamp, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return audit.query(actorId, resourceType, resourceId, eventType, fromTimestamp, toTimestamp, page, size);
    }

    @GetMapping("/verify")
    public VerifyResponse verify() {
        return audit.verify();
    }

    @PostMapping("/archive")
    public ArchiveResponse archive(@Valid @RequestBody ArchiveRequest r) {
        return ext.archive(r);
    }

    @PostMapping("/{eventId}/redactions")
    public RedactionResponse redact(@PathVariable UUID eventId, @Valid @RequestBody RedactionRequest r) {
        return ext.redact(eventId, r);
    }

    @GetMapping("/export")
    public ExportBundle export(@RequestParam(required = false) String actorId, @RequestParam(required = false) String resourceId) {
        return ext.export(actorId, resourceId);
    }

    @PostMapping("/export/verify")
    public ExportVerifyResponse verifyExport(@RequestBody ExportBundle b) {
        return ext.verifyExport(b);
    }
}
