package com.sixpay.payment.infrastructure.banking.amplitude.confirmation;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;

public interface AmplitudePaymentConfirmationClient {
    PaymentConfirmationBankResult create(PaymentConfirmationGateway.CreateRequest request);
    PaymentConfirmationBankResult verify(PaymentConfirmationGateway.VerifyRequest request);
    PaymentConfirmationBankResult replace(PaymentConfirmationGateway.ReplaceRequest request);
    PaymentConfirmationBankResult lookup(PaymentConfirmationGateway.LookupRequest request);
    PaymentConfirmationBankResult recover(PaymentConfirmationGateway.RecoveryRequest request);
    PaymentConfirmationBankResult revoke(PaymentConfirmationGateway.RevokeRequest request);
}
