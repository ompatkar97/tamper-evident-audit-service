package com.auditlog.service.dto;
import com.fasterxml.jackson.databind.JsonNode;import jakarta.validation.constraints.*;import java.time.Instant;import java.util.*;
public final class ApiDtos{private ApiDtos(){}
 public record CreateAuditEventRequest(@NotBlank String eventType,@NotBlank String actorId,@NotBlank String resourceType,@NotBlank String resourceId,@NotNull JsonNode payload,@NotNull Instant timestamp){}
 public record AuditEventResponse(UUID id,long sequenceNumber,String eventType,String actorId,String resourceType,String resourceId,JsonNode payload,Instant timestamp,String previousHash,String currentHash,boolean archived){}
 public record VerifyResponse(boolean valid,long verifiedRecords,UUID firstInconsistentRecordId,Long firstInconsistentSequence,String violationType,String message){}
 public record ArchiveRequest(@NotNull Instant cutoff,@NotBlank String requestedBy,@NotBlank String reason){}
 public record ArchiveResponse(UUID batchId,long archivedRecords){}
 public record RedactionRequest(@NotEmpty List<@NotBlank String> jsonPaths,@NotBlank String reason,@NotBlank String requestedBy){}
 public record RedactionResponse(UUID redactionId,UUID eventId,List<String> jsonPaths,String status){}
 public record ExportBundle(String exportVersion,UUID exportId,Instant generatedAt,Map<String,String> filter,List<AuditEventResponse> records,long recordCount,String manifestHash,String manifestSignature){}
 public record ExportVerifyResponse(boolean valid,String message){}
}
