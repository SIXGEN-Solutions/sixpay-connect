package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.output.banking.FundsGateway;
import com.sixpay.payment.application.port.output.banking.VerificationGateway;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;

/**
 * Narrow Amplitude client boundary for read-only payment execution checks.
 *
 * <p>This boundary owns only the bank-approved payment-check capability.
 * Posting, posting lookup, reservation, release and reversal use their own
 * dedicated clients.</p>
 */
public interface AmplitudeAccountFundsClient {

    BankingVerificationSnapshot verifyCustomerAndAccount(
            VerificationGateway.VerificationRequest request
    );

    FundsControlSnapshot checkPaymentExecution(
            FundsGateway.FundsCheckRequest request
    );
}
