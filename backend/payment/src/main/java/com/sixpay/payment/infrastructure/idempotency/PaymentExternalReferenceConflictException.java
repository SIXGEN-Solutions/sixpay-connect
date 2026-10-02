package com.sixpay.payment.infrastructure.idempotency;

/**
 * Raised when a Partner reuses an external Payment reference with different
 * canonical business content.
 */
public final class PaymentExternalReferenceConflictException
        extends RuntimeException {

    public PaymentExternalReferenceConflictException(
            String partnerIdentifier,
            String externalPaymentReference
    ) {
        super(
                "External Payment reference is already associated with "
                        + "different business content for Partner "
                        + partnerIdentifier
                        + ": "
                        + externalPaymentReference
        );
    }
}
