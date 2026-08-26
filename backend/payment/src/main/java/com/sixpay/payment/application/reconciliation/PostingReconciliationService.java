package com.sixpay.payment.application.reconciliation;

import com.sixpay.payment.domain.model.evidence.PostingNextAction;
import com.sixpay.payment.domain.model.evidence.PostingOutcome;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class PostingReconciliationService {

    private final PostingStatusQueryService queryService;
    private final Clock clock;

    public PostingReconciliationService(
            PostingStatusQueryService queryService,
            Clock clock
    ) {
        this.queryService = Objects.requireNonNull(
                queryService,
                "Posting status query service"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "Clock"
        );
    }

    public PostingReconciliationResult reconcile(
            PostingStatusQueryService.PostingStatusQuery query
    ) {
        Optional<PostingOutcomeSnapshot> lookup =
                queryService.find(query);

        if (lookup.isEmpty()) {
            return new PostingReconciliationResult(
                    ReconciliationDisposition
                            .WAIT_AND_QUERY_AGAIN,
                    Optional.empty(),
                    clock.instant()
            );
        }

        PostingOutcomeSnapshot snapshot =
                lookup.orElseThrow();

        ReconciliationDisposition disposition =
                classify(snapshot);

        return new PostingReconciliationResult(
                disposition,
                Optional.of(snapshot),
                clock.instant()
        );
    }

    private static ReconciliationDisposition classify(
            PostingOutcomeSnapshot snapshot
    ) {
        if (snapshot.outcome() == PostingOutcome.COMPLETED
                || snapshot.outcome()
                == PostingOutcome.REJECTED_NO_FINANCIAL_EFFECT) {
            return ReconciliationDisposition.RESOLVED;
        }

        if (snapshot.outcome()
                == PostingOutcome.REVERSAL_REQUIRED
                || snapshot.nextAction()
                == PostingNextAction.REQUEST_EXPLICIT_REVERSAL) {
            return ReconciliationDisposition
                    .REQUEST_EXPLICIT_REVERSAL;
        }

        if (snapshot.nextAction()
                == PostingNextAction.WAIT_FOR_CUT_CREDIT
                || snapshot.nextAction()
                == PostingNextAction.QUERY_OUTCOME) {
            return ReconciliationDisposition
                    .WAIT_AND_QUERY_AGAIN;
        }

        return ReconciliationDisposition
                .OPEN_MANUAL_RECONCILIATION;
    }
}
