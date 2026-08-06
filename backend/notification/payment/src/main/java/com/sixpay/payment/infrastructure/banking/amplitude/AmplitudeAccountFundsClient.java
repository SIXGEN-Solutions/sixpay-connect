package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.output.banking.FundsGateway;
import com.sixpay.payment.application.port.output.banking.VerificationGateway;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;

/**
 * Narrow Amplitude client boundary for Lot 5.4.1.
 *
 * <p>Posting, lookup and reversal remain owned by later sub-lots.</p>
 */
public interface AmplitudeAccountFundsClient {

    BankingVerificationSnapshot verifyCustomerAndAccount(
            VerificationGateway.VerificationRequest request
    );

    FundsControlSnapshot checkPaymentExecution(
            FundsGateway.FundsCheckRequest request
    );
}
