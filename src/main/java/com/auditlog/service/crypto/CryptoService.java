package com.auditlog.service.crypto;

import com.auditlog.service.entity.AuditEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class CryptoService {

 private static final String HASH_ALGORITHM = "SHA-256";
 private static final String HMAC_ALGORITHM = "HmacSHA256";
 private static final String HASH_VERSION = "v1";

 private final ObjectMapper objectMapper;

 public CryptoService(ObjectMapper objectMapper) {
  this.objectMapper = objectMapper;
 }

 /**
  * Converts JSON into a deterministic representation.
  *
  * Object keys are sorted recursively.
  * Array order is preserved.
  */
 public String canonical(JsonNode node) {

  if (node == null) {
   return "null";
  }

  try {
   JsonNode sortedNode = sort(node);
   return objectMapper.writeValueAsString(sortedNode);
  } catch (Exception exception) {
   throw new IllegalArgumentException(
           "Unable to canonicalize JSON payload",
           exception
   );
  }
 }

 /**
  * Recursively sorts object field names.
  *
  * Array order is intentionally preserved because
  * changing array order can change the meaning of JSON.
  */
 private JsonNode sort(JsonNode node) {

  if (node == null || node.isNull()) {
   return objectMapper.nullNode();
  }

  if (node.isObject()) {

   ObjectNode sortedObject =
           objectMapper.createObjectNode();

   ArrayList<String> fieldNames =
           new ArrayList<>();

   node.fieldNames()
           .forEachRemaining(fieldNames::add);

   Collections.sort(fieldNames);

   for (String fieldName : fieldNames) {

    sortedObject.set(
            fieldName,
            sort(node.get(fieldName))
    );
   }

   return sortedObject;
  }

  if (node.isArray()) {

   ArrayNode sortedArray =
           objectMapper.createArrayNode();

   for (JsonNode element : node) {
    sortedArray.add(sort(element));
   }

   return sortedArray;
  }

  return node;
 }

 /**
  * Calculates the integrity hash for an audit event.
  *
  * The payload supplied here must already be canonicalized.
  */
 public String eventHash(
         long sequenceNumber,
         UUID eventId,
         String eventType,
         String actorId,
         String resourceType,
         String resourceId,
         String canonicalPayload,
         Instant timestamp,
         String previousHash
 ) {

  String hashInput = frame(
          HASH_VERSION,
          String.valueOf(sequenceNumber),
          eventId.toString(),
          eventType,
          actorId,
          resourceType,
          resourceId,
          canonicalPayload,
          timestamp.toString(),
          previousHash
  );

  return sha256(hashInput);
 }

 /**
  * Recalculates a persisted event hash.
  *
  * This is used during chain verification.
  */
 public String recalc(AuditEvent event) {

  String canonicalPayload =
          canonical(event.getPayload());

  return eventHash(
          event.getSequenceNumber(),
          event.getId(),
          event.getEventType(),
          event.getActorId(),
          event.getResourceType(),
          event.getResourceId(),
          canonicalPayload,
          event.getEventTimestamp(),
          event.getPreviousHash()
  );
 }

 public String sha256(String value) {

  try {
   MessageDigest digest =
           MessageDigest.getInstance(HASH_ALGORITHM);

   byte[] hash = digest.digest(
           value.getBytes(StandardCharsets.UTF_8)
   );

   return HexFormat.of().formatHex(hash);

  } catch (Exception exception) {

   throw new IllegalStateException(
           "Unable to calculate SHA-256 hash",
           exception
   );
  }
 }

 public String hmac(
         String key,
         String value
 ) {

  try {
   Mac mac =
           Mac.getInstance(HMAC_ALGORITHM);

   SecretKeySpec secretKey =
           new SecretKeySpec(
                   key.getBytes(StandardCharsets.UTF_8),
                   HMAC_ALGORITHM
           );

   mac.init(secretKey);

   byte[] result = mac.doFinal(
           value.getBytes(StandardCharsets.UTF_8)
   );

   return HexFormat.of().formatHex(result);

  } catch (Exception exception) {

   throw new IllegalStateException(
           "Unable to calculate HMAC",
           exception
   );
  }
 }

 /**
  * Length-prefixing prevents ambiguous concatenation.
  *
  * For example:
  * ["ab", "c"] and ["a", "bc"]
  * produce different framed inputs.
  */
 private String frame(String... values) {

  StringBuilder builder =
          new StringBuilder();

  for (String value : values) {

   if (value == null) {
    value = "";
   }

   int byteLength =
           value.getBytes(StandardCharsets.UTF_8).length;

   builder
           .append(byteLength)
           .append(':')
           .append(value);
  }

  return builder.toString();
 }

 public String redactionCommitment(
         String key,
         JsonNode payload,
         List<String> jsonPaths
 ) {

  String canonicalPayload =
          canonical(payload);

  String canonicalPaths;

  try {
   canonicalPaths =
           objectMapper.writeValueAsString(jsonPaths);

  } catch (Exception exception) {

   throw new IllegalArgumentException(
           "Unable to serialize redaction paths",
           exception
   );
  }

  String commitmentInput = frame(
          "redaction-v1",
          canonicalPayload,
          canonicalPaths
  );

  return hmac(
          key,
          commitmentInput
  );
 }
}