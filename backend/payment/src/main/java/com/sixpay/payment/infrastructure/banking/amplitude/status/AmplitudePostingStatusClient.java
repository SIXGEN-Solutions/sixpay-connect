package com.sixpay.payment.infrastructure.banking.amplitude.status;

import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import java.util.Optional;

public interface AmplitudePostingStatusClient {

    Optional<PostingOutcomeSnapshot> findByIdempotencyKey(
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    );

    Optional<PostingOutcomeSnapshot> findByBankReference(
            BankingRequestContext context,
            String bankPostingReference
    );
}
