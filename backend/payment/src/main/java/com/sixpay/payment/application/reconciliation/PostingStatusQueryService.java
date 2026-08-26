package com.sixpay.payment.application.reconciliation;

import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.LookupGateway;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import java.util.Objects;
import java.util.Optional;

public final class PostingStatusQueryService {

    private final LookupGateway lookupGateway;

    public PostingStatusQueryService(
            LookupGateway lookupGateway
    ) {
        this.lookupGateway = Objects.requireNonNull(
                lookupGateway,
                "Lookup gateway"
        );
    }

    public Optional<PostingOutcomeSnapshot> find(
            PostingStatusQuery query
    ) {
        Objects.requireNonNull(
                query,
                "Posting status query"
        );

        Optional<PostingOutcomeSnapshot> byIdempotency =
                lookupGateway.findPostingByIdempotencyKey(
                        query.context(),
                        query.idempotencyKey()
                );

        if (byIdempotency.isPresent()) {
            return byIdempotency;
        }

        Optional<String> bankReference =
                query.bankPostingReference();

        if (bankReference.isEmpty()) {
            return Optional.empty();
        }

        return lookupGateway.findPostingByBankReference(
                query.context(),
                bankReference.orElseThrow()
        );
    }

    public record PostingStatusQuery(
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey,
            Optional<String> bankPostingReference
    ) {

        public PostingStatusQuery {
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );

            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Banking idempotency key"
            );

            Optional<String> reference =
                    bankPostingReference == null
                            ? Optional.empty()
                            : bankPostingReference;

            bankPostingReference = reference.map(
                    LookupGateway::requireBankReference
            );
        }

        public static PostingStatusQuery byIdempotencyKey(
                BankingRequestContext context,
                BankingIdempotencyKey idempotencyKey
        ) {
            return new PostingStatusQuery(
                    context,
                    idempotencyKey,
                    Optional.empty()
            );
        }

        public static PostingStatusQuery
        withBankReferenceFallback(
                BankingRequestContext context,
                BankingIdempotencyKey idempotencyKey,
                String bankPostingReference
        ) {
            return new PostingStatusQuery(
                    context,
                    idempotencyKey,
                    Optional.of(
                            LookupGateway.requireBankReference(
                                    bankPostingReference
                            )
                    )
            );
        }
    }
}