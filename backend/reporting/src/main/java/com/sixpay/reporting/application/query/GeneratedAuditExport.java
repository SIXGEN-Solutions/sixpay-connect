package com.sixpay.reporting.application.query;

import java.nio.file.Path;

public record GeneratedAuditExport(
        Path temporaryFile,
        long recordCount,
        String checksum
) {
}
