package com.sixpay.payment.infrastructure.partner;

public interface PartnerAccessAudit {
    void accepted(String partnerId, String externalPaymentReference, String correlationId);
    void rejected(String partnerId, PartnerRequestErrorCode code, String correlationId);
}
