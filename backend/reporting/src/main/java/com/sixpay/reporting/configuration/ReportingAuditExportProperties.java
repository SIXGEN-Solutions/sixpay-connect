package com.sixpay.reporting.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(
        prefix = "sixpay.reporting.audit-export"
)
public record ReportingAuditExportProperties(
        Path storageDirectory,
        URI retrievalBaseUri,
        Duration retention
) {
    public ReportingAuditExportProperties {
        if (storageDirectory == null) {
            throw new IllegalStateException(
                    "audit-export.storage-directory is required"
            );
        }
        if (retrievalBaseUri == null
                || !retrievalBaseUri.isAbsolute()) {
            throw new IllegalStateException(
                    "audit-export.retrieval-base-uri must be absolute"
            );
        }
        retention = retention == null
                ? Duration.ofHours(1)
                : retention;
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalStateException(
                    "audit-export.retention must be positive"
            );
        }
    }
}
