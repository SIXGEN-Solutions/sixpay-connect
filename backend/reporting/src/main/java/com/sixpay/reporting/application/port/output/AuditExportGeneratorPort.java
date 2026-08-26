package com.sixpay.reporting.application.port.output;

import com.sixpay.reporting.application.query.AuditExportJobDefinition;
import com.sixpay.reporting.application.query.GeneratedAuditExport;

public interface AuditExportGeneratorPort {
    GeneratedAuditExport generate(AuditExportJobDefinition job);
}
