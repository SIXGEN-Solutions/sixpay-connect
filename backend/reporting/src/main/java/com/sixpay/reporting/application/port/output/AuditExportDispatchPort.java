package com.sixpay.reporting.application.port.output;

import java.util.UUID;

@FunctionalInterface
public interface AuditExportDispatchPort {
    void dispatch(UUID exportId);
}
