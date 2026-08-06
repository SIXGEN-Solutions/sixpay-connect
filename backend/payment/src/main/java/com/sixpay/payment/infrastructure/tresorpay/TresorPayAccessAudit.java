package com.sixpay.payment.infrastructure.tresorpay;

public interface TresorPayAccessAudit {
    void accepted(String partnerId, String endToEndId, String correlationId);
    void rejected(String partnerId, TresorPayErrorCode code, String correlationId);
}
