package com.sixpay.payment.infrastructure.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StructuredPartnerAccessAudit implements PartnerAccessAudit {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(StructuredPartnerAccessAudit.class);

    @Override
    public void accepted(
            String partnerId,
            String externalPaymentReference,
            String correlationId
    ) {
        LOGGER.info(
                "event=partner_payment_request outcome=accepted partner={} externalPaymentReference={} correlationId={}",
                safe(partnerId),
                mask(externalPaymentReference),
                safe(correlationId)
        );
    }

    @Override
    public void rejected(
            String partnerId,
            PartnerRequestErrorCode code,
            String correlationId
    ) {
        LOGGER.warn(
                "event=partner_payment_request outcome=rejected partner={} code={} correlationId={}",
                safe(partnerId),
                code,
                safe(correlationId)
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.replaceAll("[\\r\\n]", "_");
    }

    private static String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4)
                + value.substring(value.length() - 4);
    }
}
