package com.auditlog.service.config;

import java.time.Period;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit")
public record AuditProperties(
        String genesisHash,
        String hmacKey,
        Retention retention
) {

    public record Retention(
            boolean enabled,
            Period period
    ) {
    }
}