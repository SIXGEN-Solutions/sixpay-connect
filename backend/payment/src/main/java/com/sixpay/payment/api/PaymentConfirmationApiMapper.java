package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.api.request.VerifyPaymentConfirmationRequest;
import com.sixpay.payment.api.response.PaymentConfirmationResponse;
import com.sixpay.payment.application.command.CreatePaymentConfirmationCommand;
import com.sixpay.payment.application.command.ResendPaymentConfirmationCommand;
import com.sixpay.payment.application.command.VerifyPaymentConfirmationCommand;
import com.sixpay.payment.application.query.ReadPaymentConfirmationQuery;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmationApiMapper {

    public CreatePaymentConfirmationCommand toCreateCommand(
            String paymentReference,
            CorrelationId correlationId,
            String idempotencyKey
    ) {
        return new CreatePaymentConfirmationCommand(
                PublicPaymentReference.of(paymentReference),
                correlationId,
                IdempotencyKey.of(idempotencyKey)
        );
    }

    public ReadPaymentConfirmationQuery toReadQuery(
            String paymentReference,
            CorrelationId correlationId
    ) {
        return new ReadPaymentConfirmationQuery(
                PublicPaymentReference.of(paymentReference),
                correlationId
        );
    }

    public VerifyPaymentConfirmationCommand toVerifyCommand(
            String paymentReference,
            CorrelationId correlationId,
            String idempotencyKey,
            VerifyPaymentConfirmationRequest request
    ) {
        return new VerifyPaymentConfirmationCommand(
                PublicPaymentReference.of(paymentReference),
                correlationId,
                IdempotencyKey.of(idempotencyKey),
                request.otp()
        );
    }

    public ResendPaymentConfirmationCommand toResendCommand(
            String paymentReference,
            CorrelationId correlationId,
            String idempotencyKey
    ) {
        return new ResendPaymentConfirmationCommand(
                PublicPaymentReference.of(paymentReference),
                correlationId,
                IdempotencyKey.of(idempotencyKey)
        );
    }

    public PaymentConfirmationResponse toResponse(
            PaymentConfirmationView view
    ) {
        return new PaymentConfirmationResponse(
                view.paymentReference().value(),
                view.status().name(),
                view.businessCode().name(),
                view.optionalDeliveryChannel()
                        .map(Enum::name)
                        .orElse(null),
                view.sentAt(),
                view.expiresAt(),
                view.verifiedAt()
        );
    }
}
