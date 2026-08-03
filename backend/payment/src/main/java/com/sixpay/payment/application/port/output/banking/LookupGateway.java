package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import java.util.Objects;
import java.util.Optional;

public interface LookupGateway {

    Optional<PostingOutcomeSnapshot> findPostingByIdempotencyKey(
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    );

    Optional<PostingOutcomeSnapshot> findPostingByBankReference(
            BankingRequestContext context,
            String bankPostingReference
    );

    static String requireBankReference(String value) {
        Objects.requireNonNull(value, "Bank posting reference");
        if (value.isBlank() || value.length() > 150) {
            throw new IllegalArgumentException(
                    "Bank posting reference must be non-blank "
                            + "and at most 150 characters"
            );
        }
        return value;
    }
}
