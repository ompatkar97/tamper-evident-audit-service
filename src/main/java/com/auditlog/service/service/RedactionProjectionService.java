package com.auditlog.service.service;

import com.auditlog.service.entity.RedactionRecord;
import com.auditlog.service.repository.RedactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RedactionProjectionService {

    private static final String REDACTED =
            "[REDACTED]";

    private final RedactionRepository redactions;

    public RedactionProjectionService(
            RedactionRepository redactions
    ) {
        this.redactions = redactions;
    }

    public JsonNode apply(
            UUID eventId,
            JsonNode originalPayload
    ) {

        JsonNode projectedPayload =
                originalPayload.deepCopy();

        List<RedactionRecord> records =
                redactions
                        .findByEventIdOrderByCreatedAtAsc(
                                eventId
                        );

        for (RedactionRecord record : records) {

            JsonNode paths =
                    record.getJsonPaths();

            if (!paths.isArray()) {
                continue;
            }

            for (JsonNode pathNode : paths) {

                String path =
                        pathNode.asText();

                redactPath(
                        projectedPayload,
                        path
                );
            }
        }

        return projectedPayload;
    }

    private void redactPath(
            JsonNode payload,
            String jsonPath
    ) {

        /*
         * Prototype supports top-level paths:
         *
         * $.accountNumber
         * $.personalIdentifier
         */

        if (jsonPath == null
                || !jsonPath.startsWith("$.")) {
            return;
        }

        String fieldName =
                jsonPath.substring(2);

        if (fieldName.isBlank()) {
            return;
        }

        if (payload instanceof ObjectNode objectNode
                && objectNode.has(fieldName)) {

            objectNode.put(
                    fieldName,
                    REDACTED
            );
        }
    }
}