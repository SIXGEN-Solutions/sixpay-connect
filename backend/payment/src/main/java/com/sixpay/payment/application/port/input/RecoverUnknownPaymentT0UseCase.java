package com.sixpay.payment.application.port.input;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.service.PaymentWorkflowResult;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

/**
 * Transport-neutral entry point for authoritative recovery of a durable
 * POSTING_OUTCOME_UNKNOWN Payment.
 */
public interface RecoverUnknownPaymentT0UseCase {

    PaymentWorkflowResult recoverByPaymentId(
            PaymentId paymentId,
            CorrelationId correlationId
    );

    PaymentWorkflowResult recoverByPaymentReference(
            PublicPaymentReference paymentReference,
            CorrelationId correlationId
    );
}
