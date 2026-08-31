package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.mapper;

import com.sixpay.payment.application.confirmation.PaymentConfirmationChallengeFactory;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeBinding;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.dto.*;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

public final class AmplitudePaymentConfirmationMapper {

    public AmplitudeCreateConfirmationRequest toCreate(PaymentConfirmationGateway.CreateRequest request) {
        Objects.requireNonNull(request, "Create request");
        ConfirmationChallengeBinding binding =
                PaymentConfirmationChallengeFactory.requireBinding(request.payment());
        Money amount = binding.amount();
        return new AmplitudeCreateConfirmationRequest(
                binding.paymentReference().value(),
                binding.customerReference(),
                binding.debtorAccountReference(),
                new AmplitudeCreateConfirmationRequest.Money(
                        amount.amount(),
                        amount.currency().getCurrencyCode()
                )
        );
    }

    public AmplitudeVerifyConfirmationRequest toVerify(PaymentConfirmationGateway.VerifyRequest request) {
        Objects.requireNonNull(request, "Verify request");
        return new AmplitudeVerifyConfirmationRequest(
                request.paymentReference().value(),
                new String(request.otp())
        );
    }

    public AmplitudeReplaceConfirmationRequest toReplace(PaymentConfirmationGateway.ReplaceRequest request) {
        Objects.requireNonNull(request, "Replace request");
        return new AmplitudeReplaceConfirmationRequest(request.paymentReference().value());
    }

    public AmplitudeRevokeConfirmationRequest toRevoke(PaymentConfirmationGateway.RevokeRequest request) {
        Objects.requireNonNull(request, "Revoke request");
        return new AmplitudeRevokeConfirmationRequest(
                request.paymentReference().value(),
                request.reasonCode()
        );
    }

    public PaymentConfirmationBankResult toBankResult(AmplitudeConfirmationResponse response) {
        Objects.requireNonNull(response, "Confirmation response");
        return new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference(
                        response.challengeReference()
                ),
                ConfirmationChallengeStatus.valueOf(
                        response.challengeStatus()
                ),
                ConfirmationBusinessCode.valueOf(
                        response.businessCode()
                ),
                response.deliveryChannel() == null
                        ? null
                        : ConfirmationDeliveryChannel.valueOf(
                                response.deliveryChannel()
                        ),
                response.sentAt(),
                response.expiresAt(),
                response.verifiedAt()
        );
    }
}
