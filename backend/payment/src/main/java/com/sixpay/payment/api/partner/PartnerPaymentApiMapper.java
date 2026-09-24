package com.sixpay.payment.api.partner;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.api.partner.request.InitiateDebitRequest;
import com.sixpay.payment.api.partner.response.InitiateDebitResponse;
import com.sixpay.payment.api.response.PaymentMoneyResponse;
import com.sixpay.payment.application.command.PaymentBeneficiaryCommand;
import com.sixpay.payment.application.command.InitiatePaymentCommand;
import com.sixpay.payment.application.view.PaymentInitiationResult;
import com.sixpay.payment.domain.model.CanonicalPartnerIdentity;
import com.sixpay.payment.domain.model.ExternalSubscriptionReference;
import com.sixpay.payment.domain.model.PaymentSource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Maps the partner-facing API contract to the Payment application boundary.
 */
@Component
public final class PartnerPaymentApiMapper {

    /**
     * Creates the application command while keeping authentication and
     * transport metadata outside the public request body.
     *
     * <p>{@code authenticatedPartnerLoginName}, the idempotency key and the
     * correlation ID come from trusted request processing components. The
     * command validates their relationship with the partner-provided data.</p>
     */
    public InitiatePaymentCommand toCommand(
            InitiateDebitRequest request,
            String authenticatedPartnerLoginName,
            String authenticatedPartnerSubject,
            String idempotencyKey,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(request, "InitiateDebit request");
        String authenticatedLogin = Objects.requireNonNull(
                authenticatedPartnerLoginName,
                "Authenticated partner login name"
        ).trim();
        if (!request.loginName().equals(authenticatedLogin)) {
            throw new IllegalArgumentException(
                    "Partner login name must match the authenticated partner login"
            );
        }

        CanonicalPartnerIdentity partnerIdentity =
                CanonicalPartnerIdentity.from(authenticatedPartnerSubject);

        return new InitiatePaymentCommand(
                PaymentSource.of("PARTNER"),
                externalSubscriptionReference(request),
                partnerIdentity,
                request.applicationId(),
                request.endToEndId(),
                request.totalAmount(),
                request.currency(),
                request.debtorRib(),
                request.debtorName(),
                request.claimType(),
                request.taxpayerIdentifier(),
                request.requestedExecutionAt(),
                request.beneficiaries().stream()
                        .map(beneficiary ->
                                new PaymentBeneficiaryCommand(
                                        beneficiary.rib(),
                                        beneficiary.amount()
                                )
                        )
                        .toList(),
                request.callbackUrl(),
                idempotencyKey,
                correlationId
        );
    }

    /**
     * Maps the stable application result to the partner contract.
     *
     * <p>Bank challenge fields remain {@code null} until an authoritative
     * core-banking response supplies them; SIXPAY does not synthesize bank
     * operation IDs, fees, validity periods or QR data.</p>
     */
    public InitiateDebitResponse toResponse(
            PaymentInitiationResult result
    ) {
        Objects.requireNonNull(result, "InitiateDebit result");

        return new InitiateDebitResponse(
                "200",
                "Payment order initiated successfully",
                "Success",
                result.paymentReference().value(),
                result.endToEndId(),
                null,
                money(result.totalAmount()),
                null,
                null,
                result.initiatedAt(),
                null,
                null,
                null,
                result.status().name(),
                "Awaiting customer confirmation via OTP/SMS"
        );
    }

    private static PaymentMoneyResponse money(
            com.sixpay.sharedkernel.domain.valueobject.Money money
    ) {
        return new PaymentMoneyResponse(
                money.amount(),
                money.currency().getCurrencyCode()
        );
    }
    private static ExternalSubscriptionReference externalSubscriptionReference(
            InitiateDebitRequest request
    ) {
        String value = request.applicationId() == null
                ? request.loginName()
                : request.loginName()
                + ":"
                + request.applicationId();

        return ExternalSubscriptionReference.of(value);
    }

}
