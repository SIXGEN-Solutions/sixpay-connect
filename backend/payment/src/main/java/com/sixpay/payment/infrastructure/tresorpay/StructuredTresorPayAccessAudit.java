package com.sixpay.payment.infrastructure.tresorpay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StructuredTresorPayAccessAudit implements TresorPayAccessAudit {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(StructuredTresorPayAccessAudit.class);

    @Override
    public void accepted(
            String partnerId,
            String endToEndId,
            String correlationId
    ) {
        LOGGER.info(
                "event=tresorpay_payment_request outcome=accepted partner={} endToEndId={} correlationId={}",
                safe(partnerId),
                mask(endToEndId),
                safe(correlationId)
        );
    }

    @Override
    public void rejected(
            String partnerId,
            TresorPayErrorCode code,
            String correlationId
    ) {
        LOGGER.warn(
                "event=tresorpay_payment_request outcome=rejected partner={} code={} correlationId={}",
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
