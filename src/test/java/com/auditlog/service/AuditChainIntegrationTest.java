package com.auditlog.service;

import com.auditlog.service.config.AuditProperties;
import com.auditlog.service.dto.ApiDtos.CreateAuditEventRequest;
import com.auditlog.service.dto.ApiDtos.AuditEventResponse;
import com.auditlog.service.dto.ApiDtos.VerifyResponse;
import com.auditlog.service.entity.AuditEvent;
import com.auditlog.service.repository.AuditEventRepository;
import com.auditlog.service.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.auditlog.service.dto.ApiDtos.ArchiveRequest;
import com.auditlog.service.dto.ApiDtos.ArchiveResponse;
import com.auditlog.service.dto.ApiDtos.VerifyResponse;
import com.auditlog.service.entity.AuditEvent;
import com.auditlog.service.service.ExtensionService;
import com.auditlog.service.dto.ApiDtos.RedactionRequest;
import com.auditlog.service.dto.ApiDtos.RedactionResponse;
import com.auditlog.service.dto.ApiDtos.ExportBundle;
import com.auditlog.service.dto.ApiDtos.ExportVerifyResponse;
import org.springframework.data.domain.Page;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuditChainIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditProperties auditProperties;

    @Autowired
    private ExtensionService extensionService;

    @BeforeEach
    void setUp() {

        jdbcTemplate.update(
                "DELETE FROM audit_redaction"
        );

        jdbcTemplate.update(
                "UPDATE audit_event SET archive_batch_id = NULL"
        );

        jdbcTemplate.update(
                "DELETE FROM audit_event"
        );

        jdbcTemplate.update(
                "DELETE FROM audit_archive_batch"
        );

        jdbcTemplate.update(
                """
                UPDATE chain_state
                SET last_sequence = 0,
                    last_hash = ?
                WHERE id = 1
                """,
                auditProperties.genesisHash()
        );
    }

    @Test
    void shouldCreateValidHashChain() throws Exception {

        AuditEventResponse event1 =
                auditService.create(
                        new CreateAuditEventRequest(
                                "ACCOUNT_VIEWED",
                                "EMP101",
                                "CLIENT_ACCOUNT",
                                "AC0001",
                                objectMapper.readTree(
                                        """
                                        {
                                          "operation": "VIEW",
                                          "channel": "WEB"
                                        }
                                        """
                                ),
                                Instant.parse(
                                        "2026-10-28T10:00:00Z"
                                )
                        )
                );

        AuditEventResponse event2 =
                auditService.create(
                        new CreateAuditEventRequest(
                                "ACCOUNT_UPDATED",
                                "EMP101",
                                "CLIENT_ACCOUNT",
                                "AC0001",
                                objectMapper.readTree(
                                        """
                                        {
                                          "operation": "UPDATE",
                                          "channel": "WEB"
                                        }
                                        """
                                ),
                                Instant.parse(
                                        "2026-10-28T10:01:00Z"
                                )
                        )
                );

        AuditEventResponse event3 =
                auditService.create(
                        new CreateAuditEventRequest(
                                "ACCOUNT_DOWNLOADED",
                                "EMP102",
                                "CLIENT_ACCOUNT",
                                "AC0001",
                                objectMapper.readTree(
                                        """
                                        {
                                          "operation": "DOWNLOAD",
                                          "channel": "WEB"
                                        }
                                        """
                                ),
                                Instant.parse(
                                        "2026-10-28T10:02:00Z"
                                )
                        )
                );

        /*
         * Validate sequence ordering.
         */

        assertEquals(
                1,
                event1.sequenceNumber()
        );

        assertEquals(
                2,
                event2.sequenceNumber()
        );

        assertEquals(
                3,
                event3.sequenceNumber()
        );

        /*
         * Validate hash linkage.
         */

        assertEquals(
                event1.currentHash(),
                event2.previousHash()
        );

        assertEquals(
                event2.currentHash(),
                event3.previousHash()
        );

        /*
         * Validate persisted records.
         */

        List<AuditEvent> persistedEvents =
                auditEventRepository
                        .findAllByOrderBySequenceNumberAsc();

        assertEquals(
                3,
                persistedEvents.size()
        );

        /*
         * Validate complete chain.
         */

        VerifyResponse verification =
                auditService.verify();

        assertTrue(
                verification.valid(),
                () -> "Verification failed: "
                        + "type=" + verification.violationType()
                        + ", sequence=" + verification.firstInconsistentSequence()
                        + ", message=" + verification.message()
        );

        assertEquals(
                3,
                verification.verifiedRecords()
        );

        assertNull(
                verification.firstInconsistentRecordId()
        );

        assertNull(
                verification.firstInconsistentSequence()
        );

        assertNull(
                verification.violationType()
        );
    }

    @Test
    void shouldDetectTamperedAuditRecord() throws Exception {

        // Create Event 1
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_VIEWED",
                        "EMP101",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "VIEW",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:00:00Z")
                )
        );

        // Create Event 2
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_UPDATED",
                        "EMP101",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "UPDATE",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:01:00Z")
                )
        );

        // Create Event 3
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_DOWNLOADED",
                        "EMP102",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "DOWNLOAD",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:02:00Z")
                )
        );

        // First verify that the original chain is valid.
        VerifyResponse beforeTampering =
                auditService.verify();

        assertTrue(
                beforeTampering.valid(),
                "Chain should be valid before tampering"
        );

        assertEquals(
                3,
                beforeTampering.verifiedRecords()
        );

        /*
         * Simulate an attacker bypassing the application
         * and modifying PostgreSQL directly.
         */
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE audit_event
                SET actor_id = ?
                WHERE sequence_number = ?
                """,
                "TAMPERED_USER",
                2
        );

        assertEquals(
                1,
                updatedRows,
                "Exactly one audit record should be modified"
        );

        // Verify again after tampering.
        VerifyResponse afterTampering =
                auditService.verify();

        assertFalse(
                afterTampering.valid()
        );

        assertEquals(
                1,
                afterTampering.verifiedRecords()
        );

        assertEquals(
                2L,
                afterTampering.firstInconsistentSequence()
        );

        assertEquals(
                "CURRENT_HASH_MISMATCH",
                afterTampering.violationType()
        );

        assertNotNull(
                afterTampering.firstInconsistentRecordId()
        );
    }

    @Test
    void shouldArchiveEligibleEventsAndKeepChainValid() throws Exception {

        // Event 1 - should be archived
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_VIEWED",
                        "EMP101",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "VIEW",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:00:00Z")
                )
        );

        // Event 2 - should be archived
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_UPDATED",
                        "EMP101",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "UPDATE",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:01:00Z")
                )
        );

        // Event 3 - should remain active
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_DOWNLOADED",
                        "EMP102",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "DOWNLOAD",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:02:00Z")
                )
        );

        /*
         * Confirm chain is valid before archival.
         */
        VerifyResponse beforeArchive =
                auditService.verify();

        assertTrue(
                beforeArchive.valid(),
                "Chain should be valid before archival"
        );

        /*
         * Archive records strictly before 10:02.
         *
         * Events at 10:00 and 10:01 should be archived.
         * Event at 10:02 should remain active.
         */
        ArchiveRequest archiveRequest =
                new ArchiveRequest(
                        Instant.parse("2026-10-28T10:02:00Z"),
                        "AUDIT_ADMIN",
                        "Retention integration test"
                );

        ArchiveResponse archiveResponse =
                extensionService.archive(archiveRequest);

        /*
         * Verify archive operation result.
         */
        assertNotNull(
                archiveResponse.batchId(),
                "Archive batch ID should be created"
        );

        assertEquals(
                2,
                archiveResponse.archivedRecords()
        );

        /*
         * Load persisted records.
         */
        List<AuditEvent> persistedEvents =
                auditEventRepository
                        .findAllByOrderBySequenceNumberAsc();

        assertEquals(
                3,
                persistedEvents.size()
        );

        AuditEvent event1 = persistedEvents.get(0);
        AuditEvent event2 = persistedEvents.get(1);
        AuditEvent event3 = persistedEvents.get(2);

        /*
         * Events 1 and 2 should belong to the same archive batch.
         */
        assertTrue(event1.isArchived());
        assertTrue(event2.isArchived());

        assertEquals(
                archiveResponse.batchId(),
                event1.getArchiveBatchId()
        );

        assertEquals(
                archiveResponse.batchId(),
                event2.getArchiveBatchId()
        );

        /*
         * Event 3 should remain active.
         */
        assertFalse(event3.isArchived());

        assertNull(
                event3.getArchiveBatchId()
        );

        /*
         * Most important requirement:
         *
         * legitimate archival must NOT be interpreted
         * as audit-chain tampering.
         */
        VerifyResponse afterArchive =
                auditService.verify();

        assertTrue(
                afterArchive.valid(),
                () -> "Chain should remain valid after archival. "
                        + "Violation="
                        + afterArchive.violationType()
                        + ", sequence="
                        + afterArchive.firstInconsistentSequence()
        );

        assertEquals(
                3,
                afterArchive.verifiedRecords()
        );

        assertNull(
                afterArchive.firstInconsistentRecordId()
        );

        assertNull(
                afterArchive.firstInconsistentSequence()
        );

        assertNull(
                afterArchive.violationType()
        );
    }
    @Test
    void shouldRedactSensitiveFieldAndKeepChainValid() throws Exception {

        /*
         * Create an audit event containing sensitive data.
         */
        AuditEventResponse createdEvent =
                auditService.create(
                        new CreateAuditEventRequest(
                                "ACCOUNT_VIEWED",
                                "EMP200",
                                "CLIENT_ACCOUNT",
                                "AC0099",
                                objectMapper.readTree("""
                                    {
                                      "operation": "VIEW",
                                      "channel": "WEB",
                                      "accountNumber": "123456789",
                                      "personalIdentifier": "PID-99881"
                                    }
                                    """),
                                Instant.parse(
                                        "2026-10-28T10:00:00Z"
                                )
                        )
                );

        /*
         * Verify original chain before redaction.
         */
        VerifyResponse beforeRedaction =
                auditService.verify();

        assertTrue(
                beforeRedaction.valid(),
                "Chain should be valid before redaction"
        );

        /*
         * Request field-level redaction.
         */
        RedactionRequest redactionRequest =
                new RedactionRequest(
                        List.of("$.accountNumber"),
                        "Privacy integration test",
                        "PRIVACY_ADMIN"
                );

        RedactionResponse redactionResponse =
                extensionService.redact(
                        createdEvent.id(),
                        redactionRequest
                );

        assertNotNull(
                redactionResponse.redactionId()
        );

        assertEquals(
                createdEvent.id(),
                redactionResponse.eventId()
        );

        assertEquals(
                List.of("$.accountNumber"),
                redactionResponse.jsonPaths()
        );

        assertEquals(
                "RECORDED",
                redactionResponse.status()
        );

        /*
         * Read the event through AuditService.query().
         *
         * The response should use the redacted projection.
         */
        Page<AuditEventResponse> result =
                auditService.query(
                        null,
                        null,
                        "AC0099",
                        null,
                        null,
                        null,
                        0,
                        20
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        AuditEventResponse projectedEvent =
                result.getContent().get(0);

        /*
         * Requested field should be masked.
         */
        assertEquals(
                "[REDACTED]",
                projectedEvent
                        .payload()
                        .get("accountNumber")
                        .asText()
        );

        /*
         * Unrequested sensitive field should remain unchanged.
         */
        assertEquals(
                "PID-99881",
                projectedEvent
                        .payload()
                        .get("personalIdentifier")
                        .asText()
        );

        /*
         * Verify the original database payload was NOT rewritten.
         *
         * Redaction is a read-time projection.
         */
        AuditEvent persistedEvent =
                auditEventRepository
                        .findById(createdEvent.id())
                        .orElseThrow();

        assertEquals(
                "123456789",
                persistedEvent
                        .getPayload()
                        .get("accountNumber")
                        .asText()
        );

        /*
         * Most important integrity requirement:
         * redaction must not break the hash chain.
         */
        VerifyResponse afterRedaction =
                auditService.verify();

        assertTrue(
                afterRedaction.valid(),
                () -> "Chain should remain valid after redaction. "
                        + "Violation="
                        + afterRedaction.violationType()
                        + ", sequence="
                        + afterRedaction.firstInconsistentSequence()
        );

        assertEquals(
                1,
                afterRedaction.verifiedRecords()
        );
    }

    @Test
    void shouldVerifyValidExportAndRejectTamperedExport() throws Exception {

        // Create Event 1
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_VIEWED",
                        "EMP101",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "VIEW",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:00:00Z")
                )
        );

        // Create Event 2
        auditService.create(
                new CreateAuditEventRequest(
                        "ACCOUNT_UPDATED",
                        "EMP101",
                        "CLIENT_ACCOUNT",
                        "AC0001",
                        objectMapper.readTree("""
                            {
                              "operation": "UPDATE",
                              "channel": "WEB"
                            }
                            """),
                        Instant.parse("2026-10-28T10:01:00Z")
                )
        );

        /*
         * Export by resourceId.
         *
         * actorId = null
         * resourceId = AC0001
         */
        ExportBundle exportBundle =
                extensionService.export(
                        null,
                        "AC0001"
                );

        assertNotNull(exportBundle);

        assertNotNull(
                exportBundle.exportId()
        );

        assertNotNull(
                exportBundle.manifestHash()
        );

        assertNotNull(
                exportBundle.manifestSignature()
        );

        assertEquals(
                2,
                exportBundle.recordCount()
        );

        assertEquals(
                "AC0001",
                exportBundle
                        .filter()
                        .get("resourceId")
        );

        /*
         * Original export must verify.
         */
        ExportVerifyResponse validResult =
                extensionService.verifyExport(
                        exportBundle
                );

        assertTrue(
                validResult.valid(),
                () -> "Original export should be valid: "
                        + validResult.message()
        );

        /*
         * Now simulate somebody modifying the exported bundle.
         *
         * We deliberately change actorId while keeping the
         * original manifestHash and manifestSignature.
         */

        List<AuditEventResponse> tamperedRecords =
                new ArrayList<>(
                        exportBundle.records()
                );

        AuditEventResponse original =
                tamperedRecords.get(0);

        AuditEventResponse tampered =
                new AuditEventResponse(
                        original.id(),
                        original.sequenceNumber(),
                        original.eventType(),

                        // Tampered value
                        "HACKED_USER",

                        original.resourceType(),
                        original.resourceId(),
                        original.payload(),
                        original.timestamp(),
                        original.previousHash(),
                        original.currentHash(),
                        original.archived()
                );

        tamperedRecords.set(
                0,
                tampered
        );

        /*
         * Build a modified bundle but deliberately retain
         * the ORIGINAL manifest hash and signature.
         */
        ExportBundle tamperedBundle =
                new ExportBundle(
                        exportBundle.exportVersion(),
                        exportBundle.exportId(),
                        exportBundle.generatedAt(),
                        exportBundle.filter(),
                        tamperedRecords,
                        exportBundle.recordCount(),
                        exportBundle.manifestHash(),
                        exportBundle.manifestSignature()
                );

        /*
         * Modified export must fail verification.
         */
        ExportVerifyResponse tamperedResult =
                extensionService.verifyExport(
                        tamperedBundle
                );

        assertFalse(
                tamperedResult.valid()
        );

        assertEquals(
                "Export bundle integrity check failed",
                tamperedResult.message()
        );
    }

    @Test
    void shouldCreateValidChainDuringConcurrentWrites() throws Exception {

        int numberOfWrites = 10;

        ExecutorService executorService =
                Executors.newFixedThreadPool(5);

        /*
         * All threads wait on this latch so they start
         * creating events at approximately the same time.
         */
        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<AuditEventResponse>> futures =
                new ArrayList<>();

        try {

            for (int index = 1;
                 index <= numberOfWrites;
                 index++) {

                int eventNumber = index;

                Future<AuditEventResponse> future =
                        executorService.submit(() -> {

                            startLatch.await();

                            return auditService.create(
                                    new CreateAuditEventRequest(
                                            "CONCURRENT_ACCOUNT_VIEWED",
                                            "EMP" + eventNumber,
                                            "CLIENT_ACCOUNT",
                                            "AC" + eventNumber,
                                            objectMapper.readTree(
                                                    """
                                                    {
                                                      "operation": "VIEW",
                                                      "channel": "API"
                                                    }
                                                    """
                                            ),
                                            Instant.parse(
                                                    "2026-10-28T10:00:00Z"
                                            ).plusSeconds(eventNumber)
                                    )
                            );
                        });

                futures.add(future);
            }

            /*
             * Release all waiting threads.
             */
            startLatch.countDown();

            /*
             * Wait for every concurrent request to finish.
             */
            List<AuditEventResponse> responses =
                    new ArrayList<>();

            for (Future<AuditEventResponse> future : futures) {

                responses.add(
                        future.get(
                                30,
                                TimeUnit.SECONDS
                        )
                );
            }

            assertEquals(
                    numberOfWrites,
                    responses.size()
            );

            /*
             * Every response must have a unique sequence.
             */
            Set<Long> responseSequences =
                    new HashSet<>();

            for (AuditEventResponse response : responses) {

                responseSequences.add(
                        response.sequenceNumber()
                );
            }

            assertEquals(
                    numberOfWrites,
                    responseSequences.size(),
                    "Concurrent writes must not produce duplicate sequences"
            );

            /*
             * Validate all expected sequences exist.
             *
             * Response order may differ because threads complete
             * in a nondeterministic order.
             */
            for (long sequence = 1;
                 sequence <= numberOfWrites;
                 sequence++) {

                assertTrue(
                        responseSequences.contains(sequence),
                        "Missing sequence number: " + sequence
                );
            }

            /*
             * Load persisted records in deterministic sequence order.
             */
            List<AuditEvent> persistedEvents =
                    auditEventRepository
                            .findAllByOrderBySequenceNumberAsc();

            assertEquals(
                    numberOfWrites,
                    persistedEvents.size()
            );

            /*
             * Verify sequence continuity and hash linkage.
             */
            for (int index = 0;
                 index < persistedEvents.size();
                 index++) {

                AuditEvent currentEvent =
                        persistedEvents.get(index);

                long expectedSequence =
                        index + 1L;

                assertEquals(
                        expectedSequence,
                        currentEvent.getSequenceNumber()
                );

                if (index == 0) {

                    assertEquals(
                            auditProperties.genesisHash(),
                            currentEvent.getPreviousHash()
                    );

                } else {

                    AuditEvent previousEvent =
                            persistedEvents.get(index - 1);

                    assertEquals(
                            previousEvent.getCurrentHash(),
                            currentEvent.getPreviousHash(),
                            "Chain link is invalid at sequence "
                                    + currentEvent.getSequenceNumber()
                    );
                }
            }

            /*
             * Final chain verification must succeed.
             */
            VerifyResponse verification =
                    auditService.verify();

            assertTrue(
                    verification.valid(),
                    () -> "Concurrent chain verification failed: "
                            + "type="
                            + verification.violationType()
                            + ", sequence="
                            + verification.firstInconsistentSequence()
                            + ", message="
                            + verification.message()
            );

            assertEquals(
                    numberOfWrites,
                    verification.verifiedRecords()
            );

            assertNull(
                    verification.firstInconsistentRecordId()
            );

            assertNull(
                    verification.firstInconsistentSequence()
            );

            assertNull(
                    verification.violationType()
            );

        } finally {

            executorService.shutdownNow();

            assertTrue(
                    executorService.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    ),
                    "Executor service did not terminate"
            );
        }
    }
}


