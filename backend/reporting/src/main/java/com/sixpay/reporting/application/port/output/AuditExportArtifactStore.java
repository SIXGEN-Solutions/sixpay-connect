package com.sixpay.reporting.application.port.output;

import com.sixpay.reporting.application.query.AuditExportJobDefinition;
import com.sixpay.reporting.application.query.GeneratedAuditExport;
import com.sixpay.reporting.application.query.StoredAuditExportArtifact;

public interface AuditExportArtifactStore {
    StoredAuditExportArtifact store(
            AuditExportJobDefinition job,
            GeneratedAuditExport generated
    );
}
