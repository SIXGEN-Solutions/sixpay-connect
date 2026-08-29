package com.sixpay.security.application.port.output;

import com.sixpay.security.domain.administration.SecurityAuditEvent;

@FunctionalInterface
public interface SecurityAuditPort {
    void record(SecurityAuditEvent event);
}
