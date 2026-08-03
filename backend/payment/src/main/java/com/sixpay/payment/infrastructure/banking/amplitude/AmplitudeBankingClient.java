package com.sixpay.payment.infrastructure.banking.amplitude;

import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.FundsGateway;
import com.sixpay.payment.application.port.output.banking.PostingGateway;
import com.sixpay.payment.application.port.output.banking.ReversalGateway;
import com.sixpay.payment.application.port.output.banking.VerificationGateway;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;

import java.util.Optional;

/**
 * Low-level Amplitude client contract.
 *
 * <p>No implementation is supplied in this foundation increment.</p>
 */
public interface AmplitudeBankingClient {

    BankingVerificationSnapshot verifyCustomerAndAccount(
            VerificationGateway.VerificationRequest request
    );

    FundsControlSnapshot checkPaymentExecution(
            FundsGateway.FundsCheckRequest request
    );

    PostingOutcomeSnapshot postPayment(
            PostingGateway.PostingRequest request
    );

    Optional<PostingOutcomeSnapshot> findPostingByIdempotencyKey(
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    );

    Optional<PostingOutcomeSnapshot> findPostingByBankReference(
            BankingRequestContext context,
            String bankPostingReference
    );

    ReversalSnapshot reversePayment(
            ReversalGateway.ReversalRequest request
    );
}
