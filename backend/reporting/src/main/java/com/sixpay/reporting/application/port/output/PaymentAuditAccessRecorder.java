package com.sixpay.reporting.application.port.output;

import java.util.UUID;

public interface PaymentAuditAccessRecorder {

    void recordSuccessfulRead(
            String action,
            String targetType,
            String targetId,
            UUID correlationId,
            String actorId
    );
}
