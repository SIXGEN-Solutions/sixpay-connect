package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.api.request.InitiateDebitRequest;
import com.sixpay.payment.api.response.InitiateDebitResponse;
import com.sixpay.payment.api.response.PaymentMoneyResponse;
import com.sixpay.payment.application.command.InitiateDebitBeneficiaryCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.application.view.PaymentConfirmationChallengeView;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Maps the partner-facing API contract to the Payment application boundary.
 */
@Component
public final class PaymentCommandApiMapper {

    public InitiateDebitCommand toCommand(
            InitiateDebitRequest request,
            String authenticatedPartnerLoginName,
            String idempotencyKey,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(request, "InitiateDebit request");

        return new InitiateDebitCommand(
                request.loginName(),
                authenticatedPartnerLoginName,
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
                                new InitiateDebitBeneficiaryCommand(
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

    public InitiateDebitResponse toResponse(
            InitiateDebitResult result
    ) {
        Objects.requireNonNull(result, "InitiateDebit result");

        PaymentConfirmationChallengeView challenge =
                result.confirmationChallenge();

        return new InitiateDebitResponse(
                "200",
                "Payment order initiated successfully",
                "Success",
                result.paymentReference().value(),
                result.endToEndId(),
                challenge == null
                        ? null
                        : challenge.bankOperationId(),
                money(result.totalAmount()),
                challenge == null
                        ? null
                        : money(challenge.fees()),
                challenge == null
                        ? null
                        : money(challenge.netAmount()),
                result.initiatedAt(),
                challenge == null
                        ? null
                        : challenge.validityInMinutes(),
                challenge == null
                        ? null
                        : challenge.transactionNumber(),
                challenge == null
                        ? null
                        : challenge.transactionQrCode(),
                result.status(),
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
}
