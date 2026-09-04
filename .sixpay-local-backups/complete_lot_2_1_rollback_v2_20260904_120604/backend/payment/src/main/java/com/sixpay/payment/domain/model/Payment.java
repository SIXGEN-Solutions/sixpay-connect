package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.event.*;
import com.sixpay.payment.domain.exception.PaymentDomainException;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.model.authorization.*;
import com.sixpay.payment.domain.policy.*;
import com.sixpay.payment.domain.service.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Sole write Aggregate Root for one logical TRESOR PAY payment intention.
 *
 * <p>The aggregate owns lifecycle transitions, bounded current evidence,
 * business version, timestamps, failure and Payment domain-event
 * registration. It performs no I/O and never calls another module.</p>
 */
public final class Payment {

    private static final EvidenceTemporalValidityPolicy TEMPORAL_POLICY =
            new EvidenceTemporalValidityPolicy();
    private static final BankingVerificationAcceptancePolicy
            BANKING_POLICY =
            new BankingVerificationAcceptancePolicy();
    private static final FundsControlAcceptancePolicy FUNDS_POLICY =
            new FundsControlAcceptancePolicy();
    private static final TreasuryResolutionAcceptancePolicy
            TREASURY_POLICY =
            new TreasuryResolutionAcceptancePolicy();
    private static final EvidenceReplayReplacementPolicy REPLAY_POLICY =
            new EvidenceReplayReplacementPolicy();
    private static final PostingInstructionAuthorizationPolicy
            POSTING_AUTHORIZATION_POLICY =
            new PostingInstructionAuthorizationPolicy();
    private static final PostingOutcomeDecisionService
            POSTING_DECISION_SERVICE =
            new PostingOutcomeDecisionService();
    private static final EndOfDayDecisionService TFJ_DECISION_SERVICE =
            new EndOfDayDecisionService();
    private static final ReversalDecisionService REVERSAL_DECISION_SERVICE =
            new ReversalDecisionService();
    private static final FailureClassificationPolicy FAILURE_POLICY =
            new FailureClassificationPolicy();
    private static final PaymentResultIntentService RESULT_INTENT_SERVICE =
            new PaymentResultIntentService();

    private PaymentState state;
    private final List<PaymentDomainEvent> domainEvents;

    private Payment(
            PaymentState state,
            List<PaymentDomainEvent> domainEvents
    ) {
        this.state = Objects.requireNonNull(state, "Payment state");
        this.domainEvents = new ArrayList<>(
                Objects.requireNonNull(domainEvents, "Domain events")
        );
    }

    /**
     * Creates one new Payment and registers {@link PaymentReceived}.
     */
    public static Payment receive(
            PaymentId paymentId,
            PublicPaymentReference publicPaymentReference,
            NewPaymentIntent intent,
            Instant receivedAt
    ) {
        return receive(
                paymentId,
                publicPaymentReference,
                intent,
                intent.initiationContext(),
                receivedAt
        );
    }

    public static Payment receive(
            PaymentId paymentId,
            PublicPaymentReference publicPaymentReference,
            NewPaymentIntent intent,
            PaymentInitiationContext initiationContext,
            Instant receivedAt
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        Objects.requireNonNull(intent, "New Payment intent");
        Objects.requireNonNull(receivedAt, "Received instant");

        PaymentState state = PaymentState.builder()
                .paymentId(paymentId)
                .publicPaymentReference(publicPaymentReference)
                .source(intent.source())
                .externalPaymentReference(
                        intent.externalPaymentReference()
                )
                .externalSubscriptionReference(
                        intent.externalSubscriptionReference()
                )
                .requestIdentity(intent.requestIdentity())
                .financialInstitutionCode(
                        intent.financialInstitutionCode()
                )
                .debtorAccountReference(
                        intent.debtorAccountReference()
                )
                .requestedAmount(intent.requestedAmount())
                .treasuryAllocationIntent(
                        intent.treasuryAllocationIntent()
                )
                .allocationIntentFingerprint(
                        intent.allocationIntentFingerprint()
                )
                .initiationContext(initiationContext)
                .status(PaymentStatus.RECEIVED)
                .businessVersion(1L)
                .receivedAt(receivedAt)
                .updatedAt(receivedAt)
                .finalizedAt(null)
                .build();

        EventBatch batch = new EventBatch(state, receivedAt);
        PaymentReceived event = new PaymentReceived(
                batch.metadata(),
                state.externalPaymentReference(),
                state.source(),
                state.financialInstitutionCode(),
                MoneyPayload.from(state.requestedAmount()),
                state.debtorAccountReference().maskedDisplay(),
                receivedAt
        );

        return new Payment(state, List.of(event));
    }

    /**
     * Restores persisted state without transition, version change or event.
     */
    public static Payment reconstitute(PaymentState state) {
        return new Payment(
                Objects.requireNonNull(state, "Payment state"),
                List.of()
        );
    }

    /**
     * Starts banking customer/account verification after durable reception.
     */
    public void startBankingVerification(
            Instant requestedAt
    ) {
        Objects.requireNonNull(
                requestedAt,
                "Banking-verification request instant"
        );

        if (state.status()
                == PaymentStatus.BANKING_VERIFICATION_PENDING) {
            return;
        }

        requireStatus(
                "startBankingVerification",
                PaymentStatus.RECEIVED
        );

        PaymentState next = nextBuilder(
                PaymentStatus.BANKING_VERIFICATION_PENDING,
                requestedAt
        ).failure(null).build();

        EventBatch batch = new EventBatch(next, requestedAt);
        commit(
                next,
                List.of(
                        new PaymentBankingVerificationRequested(
                                batch.metadata(),
                                next.financialInstitutionCode(),
                                next.debtorAccountReference()
                                        .bindingFingerprint(),
                                requestedAt
                        )
                )
        );
    }

    /**
     * Records the latest authoritative confirmation-challenge snapshot.
     *
     * <p>The challenge remains subordinate to this Payment. A different
     * challenge reference replaces the previously current snapshot, which
     * guarantees that SIXPAY retains at most one current ACTIVE challenge
     * for a Payment. Lifecycle status itself is supplied authoritatively by
     * Core Banking; SIXPAY does not calculate expiry or lockout locally.</p>
     *
     * <p>Every replacement must preserve the original Payment/customer/account/
     * amount binding. No OTP material enters this operation.</p>
     */
    public void recordConfirmationChallenge(
            ConfirmationChallenge challenge,
            Instant observedAt
    ) {
        Objects.requireNonNull(
                challenge,
                "Confirmation challenge"
        );
        Objects.requireNonNull(
                observedAt,
                "Confirmation challenge observation instant"
        );

        requireStatus(
                "recordConfirmationChallenge",
                PaymentStatus.PENDING_CONFIRMATION
        );

        ConfirmationChallenge current =
                state.confirmationChallenge().orElse(null);

        if (challenge.equals(current)) {
            return;
        }

        if (current != null
                && !current.binding().equals(challenge.binding())) {
            throw PaymentDomainException.conflict(
                    "Confirmation challenge binding cannot change"
            );
        }

        PaymentState next = nextBuilder(
                PaymentStatus.PENDING_CONFIRMATION,
                observedAt
        )
                .confirmationChallenge(challenge)
                .failure(null)
                .build();

        commit(next, List.of());
    }

    public void recordCustomerConfirmation(
            ConfirmationChallenge verifiedChallenge
    ) {
        Objects.requireNonNull(
                verifiedChallenge,
                "Verified confirmation challenge"
        );

        if (verifiedChallenge.status()
                != ConfirmationChallengeStatus.VERIFIED) {
            throw PaymentDomainException.conflict(
                    "Customer confirmation requires a VERIFIED challenge"
            );
        }

        Instant confirmedAt = verifiedChallenge.optionalVerifiedAt()
                .orElseThrow(() -> PaymentDomainException.conflict(
                        "VERIFIED confirmation challenge requires verifiedAt"
                ));

        if (state.status()
                == PaymentStatus.AUTHORIZATION_CHECKING) {
            if (state.confirmationChallenge()
                    .filter(verifiedChallenge::equals)
                    .isPresent()) {
                return;
            }
            throw PaymentDomainException.conflict(
                    "Conflicting verified confirmation challenge"
            );
        }

        requireStatus(
                "recordCustomerConfirmation",
                PaymentStatus.PENDING_CONFIRMATION
        );

        ConfirmationChallenge current =
                state.confirmationChallenge().orElse(null);

        if (current != null
                && !current.binding().equals(verifiedChallenge.binding())) {
            throw PaymentDomainException.conflict(
                    "Confirmation challenge binding cannot change"
            );
        }

        PaymentState next = nextBuilder(
                PaymentStatus.AUTHORIZATION_CHECKING,
                confirmedAt
        )
                .confirmationChallenge(verifiedChallenge)
                .failure(null)
                .build();

        EventBatch batch = new EventBatch(next, confirmedAt);
        commit(
                next,
                List.of(
                        new PaymentCustomerConfirmationRecorded(
                                batch.metadata(),
                                confirmedAt
                        ),
                        new PaymentAuthorizationCheckingStarted(
                                batch.metadata(),
                                confirmedAt
                        )
                )
        );
    }

    public void recordCustomerConfirmation(
            CustomerConfirmationEvidence evidence
    ) {
        Objects.requireNonNull(
                evidence,
                "Customer confirmation evidence"
        );

        if (state.status()
                == PaymentStatus.AUTHORIZATION_CHECKING) {
            if (state.customerConfirmationEvidence()
                    .filter(evidence::equals)
                    .isPresent()) {
                return;
            }

            throw PaymentDomainException.conflict(
                    "Conflicting customer confirmation evidence"
            );
        }

        requireStatus(
                "recordCustomerConfirmation",
                PaymentStatus.PENDING_CONFIRMATION
        );

        Instant confirmedAt = evidence.confirmedAt();
        PaymentState next = nextBuilder(
                PaymentStatus.AUTHORIZATION_CHECKING,
                confirmedAt
        )
                .customerConfirmationEvidence(evidence)
                .failure(null)
                .build();

        EventBatch batch = new EventBatch(next, confirmedAt);
        commit(
                next,
                List.of(
                        new PaymentCustomerConfirmationRecorded(
                                batch.metadata(),
                                confirmedAt
                        ),
                        new PaymentAuthorizationCheckingStarted(
                                batch.metadata(),
                                confirmedAt
                        )
                )
        );
    }

    public void recordCustomerConfirmation(
            Instant confirmedAt
    ) {
        Objects.requireNonNull(
                confirmedAt,
                "Customer-confirmation instant"
        );

        if (state.status()
                == PaymentStatus.AUTHORIZATION_CHECKING) {
            return;
        }
        requireStatus(
                "recordCustomerConfirmation",
                PaymentStatus.PENDING_CONFIRMATION
        );

        PaymentState next = nextBuilder(
                PaymentStatus.AUTHORIZATION_CHECKING,
                confirmedAt
        ).failure(null).build();

        EventBatch batch = new EventBatch(next, confirmedAt);
        commit(
                next,
                List.of(
                        new PaymentCustomerConfirmationRecorded(
                                batch.metadata(),
                                confirmedAt
                        ),
                        new PaymentAuthorizationCheckingStarted(
                                batch.metadata(),
                                confirmedAt
                        )
                )
        );
    }

    public void startAuthorizationChecking(Instant startedAt) {
        Objects.requireNonNull(startedAt, "Started instant");

        if (state.status() == PaymentStatus.AUTHORIZATION_CHECKING) {
            return;
        }
        requireStatus(
                "startAuthorizationChecking",
                PaymentStatus.PENDING_CONFIRMATION
        );

        recordCustomerConfirmation(startedAt);
    }

    public void recordAuthorizationDecision(
            SixpayAuthorizationDecisionSnapshot decision,
            PaymentFailure rejectionFailure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(
                decision,
                "SIXPAY authorization decision"
        );
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (state.authorizationDecision()
                .filter(decision::equals)
                .isPresent()) {
            return;
        }
        if (state.authorizationDecision().isPresent()) {
            throw PaymentDomainException.conflict(
                    "Conflicting SIXPAY authorization decision"
            );
        }

        requireStatus(
                "recordAuthorizationDecision",
                PaymentStatus.AUTHORIZATION_CHECKING
        );

        if (!decision.decidedAt().equals(decisionAt)) {
            throw PaymentDomainException.conflict(
                    "Authorization decision instant must match recorded decision"
            );
        }

        if (decision.decision()
                == SixpayAuthorizationDecision.APPROVED) {
            PaymentState next = nextBuilder(
                    PaymentStatus.FUNDS_CONTROL_PENDING,
                    decisionAt
            ).authorizationDecision(decision)
                    .failure(null)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            new PaymentFundsControlRequested(
                                    batch.metadata(),
                                    next.financialInstitutionCode(),
                                    MoneyPayload.from(
                                            next.requestedAmount()
                                    ),
                                    next.debtorAccountReference()
                                            .bindingFingerprint(),
                                    decisionAt
                            )
                    )
            );
            return;
        }

        PaymentFailure failure = requireRejectionFailure(
                rejectionFailure,
                "SIXPAY authorization rejection"
        );

        FailureCode decisionCode = decision.rejectionCode()
                .orElseThrow();
        if (!decisionCode.equals(failure.failureCode())) {
            throw PaymentDomainException.conflict(
                    "Authorization decision and Payment failure codes differ"
            );
        }
        if (failure.failureStage() != FailureStage.AUTHORIZATION) {
            throw PaymentDomainException.conflict(
                    "SIXPAY authorization rejection must use AUTHORIZATION stage"
            );
        }
        if (failure.externalSystemOptional().isPresent()) {
            throw PaymentDomainException.conflict(
                    "SIXPAY-local authorization rejection must not name an external system"
            );
        }

        PaymentState next = nextBuilder(
                PaymentStatus.REJECTED,
                decisionAt
        ).authorizationDecision(decision)
                .failure(failure)
                .build();

        EventBatch batch = new EventBatch(next, decisionAt);
        commit(
                next,
                List.of(
                        rejected(batch, failure, decisionAt),
                        immediateResult(
                                batch,
                                PaymentStatus.AUTHORIZATION_CHECKING,
                                next.status(),
                                failure,
                                decisionAt,
                                profiles
                        )
                )
        );
    }

    public void recordBankingVerification(
            BankingVerificationSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(evidence, "Banking evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (sameBankingEvidence(evidence)) {
            return;
        }
        if (state.bankingVerificationEvidence().isPresent()) {
            throw PaymentDomainException.conflict(
                    "Conflicting banking-verification evidence"
            );
        }
        requireStatus(
                "recordBankingVerification",
                PaymentStatus.BANKING_VERIFICATION_PENDING
        );

        PolicyDecision<EvidenceAcceptanceDecision> decision =
                BANKING_POLICY.decide(
                        new PaymentBankingContext(
                                state.financialInstitutionCode(),
                                state.debtorAccountReference()
                                        .bindingFingerprint()
                        ),
                        evidence,
                        decisionAt,
                        profiles.bankingVerificationPolicyProfile()
                );

        if (evidence.outcome()
                == BankingVerificationOutcome.VERIFIED) {
            requireDecision(
                    decision.decision()
                            == EvidenceAcceptanceDecision.ACCEPT,
                    decision.reasonCode()
            );

            PaymentState next = nextBuilder(
                    PaymentStatus.PENDING_CONFIRMATION,
                    decisionAt
            ).bankingVerificationEvidence(evidence)
                    .failure(null)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            bankingRecorded(batch, evidence),
                            new PaymentCustomerConfirmationRequested(
                                    batch.metadata(),
                                    decisionAt
                            )
                    )
            );
            return;
        }

        if (evidence.outcome()
                == BankingVerificationOutcome.REJECTED) {
            requireDecision(
                    decision.decision()
                            == EvidenceAcceptanceDecision.REJECT,
                    decision.reasonCode()
            );
            PaymentFailure rejection = requireRejectionFailure(
                    failure,
                    "Banking rejection"
            );

            PaymentState next = nextBuilder(
                    PaymentStatus.REJECTED,
                    decisionAt
            ).bankingVerificationEvidence(evidence)
                    .failure(rejection)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            bankingRecorded(batch, evidence),
                            rejected(batch, rejection, decisionAt),
                            immediateResult(
                                    batch,
                                    PaymentStatus
                                            .BANKING_VERIFICATION_PENDING,
                                    next.status(),
                                    rejection,
                                    decisionAt,
                                    profiles
                            )
                    )
            );
            return;
        }

        requireDecision(
                decision.decision()
                        == EvidenceAcceptanceDecision.INDETERMINATE,
                decision.reasonCode()
        );
        PaymentFailure deferred = requireRecoverableFailure(
                failure,
                "Indeterminate banking verification"
        );

        PaymentState next = nextBuilder(
                PaymentStatus.BANKING_VERIFICATION_PENDING,
                decisionAt
        ).bankingVerificationEvidence(evidence)
                .failure(deferred)
                .build();

        EventBatch batch = new EventBatch(next, decisionAt);
        commit(
                next,
                List.of(
                        bankingRecorded(batch, evidence),
                        processingDeferred(batch, deferred, decisionAt),
                        immediateResult(
                                batch,
                                PaymentStatus
                                        .BANKING_VERIFICATION_PENDING,
                                next.status(),
                                deferred,
                                decisionAt,
                                profiles,
                                true
                        )
                )
        );
    }

    public void recordFundsControl(
            FundsControlSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(evidence, "Funds-control evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (sameFundsEvidence(evidence)) {
            return;
        }
        if (state.fundsControlEvidence().isPresent()) {
            throw PaymentDomainException.conflict(
                    "Conflicting funds-control evidence"
            );
        }
        requireStatus(
                "recordFundsControl",
                PaymentStatus.FUNDS_CONTROL_PENDING
        );

        PolicyDecision<EvidenceAcceptanceDecision> decision =
                FUNDS_POLICY.decide(
                        new PaymentFundsContext(
                                state.financialInstitutionCode(),
                                state.debtorAccountReference()
                                        .bindingFingerprint(),
                                state.requestedAmount()
                        ),
                        evidence,
                        decisionAt,
                        profiles.fundsControlPolicyProfile()
                );

        if (evidence.outcome() == FundsControlOutcome.VERIFIED) {
            requireDecision(
                    decision.decision()
                            == EvidenceAcceptanceDecision.ACCEPT,
                    decision.reasonCode()
            );

            PaymentState next = nextBuilder(
                    PaymentStatus
                            .TREASURY_ACCOUNT_RESOLUTION_PENDING,
                    decisionAt
            ).fundsControlEvidence(evidence)
                    .failure(null)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            fundsRecorded(batch, evidence),
                            new PaymentTreasuryAccountResolutionRequested(
                                    batch.metadata(),
                                    next.financialInstitutionCode(),
                                    next.allocationIntentFingerprint(),
                                    decisionAt
                            )
                    )
            );
            return;
        }

        if (evidence.outcome() == FundsControlOutcome.REJECTED) {
            requireDecision(
                    decision.decision()
                            == EvidenceAcceptanceDecision.REJECT,
                    decision.reasonCode()
            );
            PaymentFailure rejection = requireRejectionFailure(
                    failure,
                    "Funds-control rejection"
            );

            PaymentState next = nextBuilder(
                    PaymentStatus.REJECTED,
                    decisionAt
            ).fundsControlEvidence(evidence)
                    .failure(rejection)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            fundsRecorded(batch, evidence),
                            rejected(batch, rejection, decisionAt),
                            immediateResult(
                                    batch,
                                    PaymentStatus.FUNDS_CONTROL_PENDING,
                                    next.status(),
                                    rejection,
                                    decisionAt,
                                    profiles
                            )
                    )
            );
            return;
        }

        requireDecision(
                decision.decision()
                        == EvidenceAcceptanceDecision.INDETERMINATE,
                decision.reasonCode()
        );
        PaymentFailure deferred = requireRecoverableFailure(
                failure,
                "Indeterminate funds control"
        );

        PaymentState next = nextBuilder(
                PaymentStatus.FUNDS_CONTROL_PENDING,
                decisionAt
        ).fundsControlEvidence(evidence)
                .failure(deferred)
                .build();

        EventBatch batch = new EventBatch(next, decisionAt);
        commit(
                next,
                List.of(
                        fundsRecorded(batch, evidence),
                        processingDeferred(batch, deferred, decisionAt),
                        immediateResult(
                                batch,
                                PaymentStatus.FUNDS_CONTROL_PENDING,
                                next.status(),
                                deferred,
                                decisionAt,
                                profiles,
                                true
                        )
                )
        );
    }

    public void recordTreasuryAccountResolution(
            TreasuryAccountResolutionSnapshot evidence,
            TreasuryAccountReference resolvedAccount,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(evidence, "Treasury-resolution evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (sameTreasuryEvidence(evidence)) {
            return;
        }
        if (state.treasuryResolutionEvidence().isPresent()) {
            throw PaymentDomainException.conflict(
                    "Conflicting Treasury-resolution evidence"
            );
        }
        requireStatus(
                "recordTreasuryAccountResolution",
                PaymentStatus.TREASURY_ACCOUNT_RESOLUTION_PENDING
        );

        PolicyDecision<EvidenceTemporalDecision> temporal =
                TEMPORAL_POLICY.decide(
                        evidence.metadata(),
                        EvidenceCategory.TREASURY_RESOLUTION,
                        decisionAt,
                        profiles.evidenceTemporalProfile()
                );
        requireDecision(
                temporal.decision() == EvidenceTemporalDecision.VALID,
                temporal.reasonCode()
        );

        PolicyDecision<EvidenceAcceptanceDecision> decision =
                TREASURY_POLICY.decide(
                        new PaymentTreasuryContext(
                                state.financialInstitutionCode(),
                                state.allocationIntentFingerprint()
                        ),
                        evidence,
                        resolvedAccount,
                        decisionAt,
                        profiles.treasuryResolutionPolicyProfile()
                );

        if (evidence.resolutionOutcome()
                == TreasuryResolutionOutcome.RESOLVED) {
            requireDecision(
                    decision.decision()
                            == EvidenceAcceptanceDecision.ACCEPT,
                    decision.reasonCode()
            );
            TreasuryAccountReference accepted =
                    Objects.requireNonNull(
                            resolvedAccount,
                            "Resolved Treasury account"
                    );

            PaymentState next = nextBuilder(
                    PaymentStatus.APPROVED_FOR_POSTING,
                    decisionAt
            ).treasuryResolutionEvidence(evidence)
                    .treasuryAccountReference(accepted)
                    .failure(null)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            treasuryRecorded(
                                    batch,
                                    evidence,
                                    accepted,
                                    null
                            ),
                            new PaymentApprovedForPosting(
                                    batch.metadata(),
                                    next.financialInstitutionCode(),
                                    MoneyPayload.from(
                                            next.requestedAmount()
                                    ),
                                    decisionAt
                            )
                    )
            );
            return;
        }

        requireDecision(
                decision.decision()
                        == EvidenceAcceptanceDecision.REJECT,
                decision.reasonCode()
        );
        PaymentFailure rejection = requireRejectionFailure(
                failure,
                "Treasury-resolution rejection"
        );

        PaymentState next = nextBuilder(
                PaymentStatus.REJECTED,
                decisionAt
        ).treasuryResolutionEvidence(evidence)
                .failure(rejection)
                .build();

        EventBatch batch = new EventBatch(next, decisionAt);
        commit(
                next,
                List.of(
                        treasuryRecorded(
                                batch,
                                evidence,
                                null,
                                rejection
                        ),
                        rejected(batch, rejection, decisionAt),
                        immediateResult(
                                batch,
                                PaymentStatus
                                        .TREASURY_ACCOUNT_RESOLUTION_PENDING,
                                next.status(),
                                rejection,
                                decisionAt,
                                profiles
                        )
                )
        );
    }

    public void authorizePosting(
            PostingInstructionIdentity instruction,
            Instant authorizedAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(instruction, "Posting instruction");
        Objects.requireNonNull(authorizedAt, "Posting authorization instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (state.postingInstruction().isPresent()) {
            PostingInstructionIdentity current =
                    state.postingInstruction().orElseThrow();
            if (current.equals(instruction)) {
                return;
            }
            throw PaymentDomainException.conflict(
                    "A different posting instruction is already authorized"
            );
        }
        requireStatus(
                "authorizePosting",
                PaymentStatus.APPROVED_FOR_POSTING
        );

        if (!instruction.amount().equals(state.requestedAmount())
                || !instruction.accountBindingFingerprint().equals(
                        state.debtorAccountReference()
                                .bindingFingerprint()
                )) {
            throw PaymentDomainException.conflict(
                    "Posting instruction is not bound to Payment"
            );
        }

        boolean fundsFresh = state.fundsControlEvidence()
                .map(snapshot ->
                        !authorizedAt.isAfter(snapshot.validUntil()))
                .orElse(false);

        PolicyDecision<PostingAuthorizationDecision> decision =
                POSTING_AUTHORIZATION_POLICY.decide(
                        new PaymentPostingAuthorizationContext(
                                state.status(),
                                state.authorizationDecision()
                                        .filter(authorizationDecision ->
                                                authorizationDecision.decision()
                                                        == SixpayAuthorizationDecision.APPROVED)
                                        .isPresent(),
                                state.bankingVerificationEvidence()
                                        .isPresent(),
                                state.fundsControlEvidence().isPresent(),
                                fundsFresh,
                                state.treasuryAccountReference()
                                        .isPresent(),
                                null
                        ),
                        instruction,
                        authorizedAt,
                        profiles.postingAuthorizationPolicyProfile()
                );

        requireDecision(
                decision.decision()
                        == PostingAuthorizationDecision.AUTHORIZE,
                decision.reasonCode()
        );

        PaymentState next = nextBuilder(
                PaymentStatus.POSTING_PENDING,
                authorizedAt
        ).postingInstruction(instruction)
                .failure(null)
                .build();

        EventBatch batch = new EventBatch(next, authorizedAt);
        commit(
                next,
                List.of(
                        new PaymentPostingAuthorized(
                                batch.metadata(),
                                instruction.instructionId(),
                                instruction.idempotencyKey(),
                                instruction.instructionFingerprint(),
                                authorizedAt
                        ),
                        new PaymentPostingRequested(
                                batch.metadata(),
                                instruction.instructionId(),
                                instruction.idempotencyKey(),
                                instruction.instructionFingerprint(),
                                next.financialInstitutionCode(),
                                MoneyPayload.from(
                                        next.requestedAmount()
                                ),
                                authorizedAt
                        )
                )
        );
    }

    public void recordPostingOutcome(
            PostingOutcomeSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        applyPostingOutcome(
                evidence,
                failure,
                decisionAt,
                profiles,
                false
        );
    }

    public void resolvePostingOutcome(
            PostingOutcomeSnapshot authoritativeEvidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        applyPostingOutcome(
                authoritativeEvidence,
                failure,
                decisionAt,
                profiles,
                true
        );
    }

    public void recordMatchedEndOfDayConfirmation(
            EndOfDayConfirmationSnapshot evidence,
            UniqueTfjMatchProof matchProof,
            PaymentFailure reconciliationFailure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(evidence, "TFJ evidence");
        Objects.requireNonNull(matchProof, "Unique TFJ match proof");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (sameTfjEvidence(evidence)) {
            return;
        }
        requireStatus(
                "recordMatchedEndOfDayConfirmation",
                PaymentStatus.POSTED_PENDING_TFJ
        );

        PolicyDecision<EvidenceTemporalDecision> temporal =
                TEMPORAL_POLICY.decide(
                        evidence.metadata(),
                        EvidenceCategory.TFJ_CONFIRMATION,
                        decisionAt,
                        profiles.evidenceTemporalProfile()
                );
        requireDecision(
                temporal.decision() == EvidenceTemporalDecision.VALID,
                temporal.reasonCode()
        );

        CurrentTfjEvidence current = state
                .endOfDayConfirmationEvidence()
                .map(existing -> new CurrentTfjEvidence(
                        evidenceIdentity(
                                existing.confirmationId().toString(),
                                existing.metadata()
                                        .evidenceFingerprint()
                        ),
                        EvidenceAuthority.UNIQUE_TFJ_MATCH,
                        EvidenceConclusiveness.FINAL
                ))
                .orElse(null);

        EndOfDayDecisionInput input = new EndOfDayDecisionInput(
                new PaymentTfjContext(
                        state.financialInstitutionCode(),
                        requireBusinessDate(),
                        state.publicPaymentReference(),
                        requirePrincipalPostingReference()
                ),
                evidence,
                matchProof,
                reconciliationFailure,
                current,
                EvidenceAuthority.UNIQUE_TFJ_MATCH,
                EvidenceConclusiveness.FINAL,
                PaymentLifecycleContext.of(state.status()),
                decisionAt
        );

        PolicyDecision<EndOfDayDecision> decision =
                TFJ_DECISION_SERVICE.decide(input, profiles);

        if (decision.decision() == EndOfDayDecision.NO_OP) {
            return;
        }
        if (decision.decision()
                == EndOfDayDecision.QUARANTINE_CONFLICT) {
            throw PaymentDomainException.conflict(
                    decision.reasonCode()
            );
        }

        if (decision.decision()
                == EndOfDayDecision.TREASURY_INTEGRATED) {
            PaymentState next = nextBuilder(
                    PaymentStatus.TREASURY_INTEGRATED,
                    decisionAt
            ).endOfDayConfirmationEvidence(evidence)
                    .failure(null)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            tfjRecorded(batch, evidence),
                            new TreasuryIntegrationConfirmed(
                                    batch.metadata(),
                                    evidence.confirmationId(),
                                    requirePrincipalPostingReference(),
                                    evidence.businessDate(),
                                    evidence.confirmedAt()
                            ),
                            new PaymentFinalResultAvailable(
                                    batch.metadata(),
                                    next.externalPaymentReference(),
                                    PaymentFinalResultType
                                            .TREASURY_INTEGRATED,
                                    requirePrincipalPostingReference(),
                                    evidence.businessDate(),
                                    evidence.confirmationId(),
                                    decisionAt
                            )
                    )
            );
            return;
        }

        PaymentFailure acceptedFailure =
                requireTfjFailure(
                        reconciliationFailure,
                        evidence
                );

        if (decision.decision()
                == EndOfDayDecision.REVERSAL_REQUIRED) {
            PaymentState next = nextBuilder(
                    PaymentStatus.REVERSAL_REQUIRED,
                    decisionAt
            ).endOfDayConfirmationEvidence(evidence)
                    .failure(acceptedFailure)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            tfjRecorded(batch, evidence),
                            reversalRequired(
                                    batch,
                                    acceptedFailure.failureCode(),
                                    ReversalSourceStage.TFJ,
                                    decisionAt
                            ),
                            immediateResult(
                                    batch,
                                    PaymentStatus.POSTED_PENDING_TFJ,
                                    next.status(),
                                    acceptedFailure,
                                    decisionAt,
                                    profiles
                            )
                    )
            );
            return;
        }

        PaymentState next = nextBuilder(
                PaymentStatus.POSTED_PENDING_TFJ,
                decisionAt
        ).endOfDayConfirmationEvidence(evidence)
                .failure(acceptedFailure)
                .build();

        TfjFailureEvidence tfjFailure = evidence.failureEvidence()
                .orElseThrow();
        EventBatch batch = new EventBatch(next, decisionAt);
        commit(
                next,
                List.of(
                        tfjRecorded(batch, evidence),
                        new PaymentTreasuryReconciliationRequired(
                                batch.metadata(),
                                evidence.confirmationId(),
                                requirePrincipalPostingReference(),
                                evidence.businessDate(),
                                tfjFailure.code(),
                                tfjFailure.recoveryAction(),
                                decisionAt
                        )
                )
        );
    }

    public void authorizeReversal(
            ReversalInstructionIdentity instruction,
            ReversalAuthorizationEvidence authorization,
            Instant authorizedAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(instruction, "Reversal instruction");
        Objects.requireNonNull(authorization, "Reversal authorization");
        Objects.requireNonNull(authorizedAt, "Authorization instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (state.reversalInstruction().isPresent()) {
            if (state.reversalInstruction().orElseThrow()
                    .equals(instruction)) {
                return;
            }
            throw PaymentDomainException.conflict(
                    "A different reversal instruction already exists"
            );
        }
        requireStatus(
                "authorizeReversal",
                PaymentStatus.REVERSAL_REQUIRED
        );

        ReversalDecisionInput input = new ReversalDecisionInput(
                new PaymentReversalEligibilityContext(
                        state.status(),
                        FinancialEffectKnowledge.CONFIRMED_PARTIAL,
                        null
                ),
                null,
                instruction,
                authorization,
                null,
                null,
                null,
                EvidenceAuthority.DIRECT_RESPONSE,
                EvidenceConclusiveness.CONCLUSIVE,
                PaymentLifecycleContext.of(state.status()),
                authorizedAt
        );

        PolicyDecision<ReversalDecision> decision =
                REVERSAL_DECISION_SERVICE.decide(input, profiles);
        requireDecision(
                decision.decision() == ReversalDecision.AUTHORIZE,
                decision.reasonCode()
        );

        BankPostingReference original =
                state.bankPostingReference().orElseThrow();
        ReversalSnapshot pending = new ReversalSnapshot(
                original,
                instruction.instructionId(),
                instruction.idempotencyKey(),
                authorization,
                null
        );

        PaymentState next = nextBuilder(
                PaymentStatus.REVERSAL_PENDING,
                authorizedAt
        ).reversalInstruction(instruction)
                .reversalAuthorizationEvidence(authorization)
                .reversalEvidence(pending)
                .build();

        EventBatch batch = new EventBatch(next, authorizedAt);
        commit(
                next,
                List.of(
                        new PaymentReversalAuthorized(
                                batch.metadata(),
                                instruction.instructionId(),
                                instruction.idempotencyKey(),
                                original.principalPostingReference(),
                                authorization.authorizationType(),
                                authorization.authorizationReference(),
                                authorization.reasonCode(),
                                authorizedAt
                        ),
                        new PaymentReversalRequested(
                                batch.metadata(),
                                instruction.instructionId(),
                                instruction.idempotencyKey(),
                                original.principalPostingReference(),
                                instruction.instructionFingerprint(),
                                authorizedAt
                        )
                )
        );
    }

    public void recordReversalOutcome(
            ReversalSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        applyReversalOutcome(
                evidence,
                failure,
                decisionAt,
                profiles,
                false
        );
    }

    public void resolveReversalOutcome(
            ReversalSnapshot authoritativeEvidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        applyReversalOutcome(
                authoritativeEvidence,
                failure,
                decisionAt,
                profiles,
                true
        );
    }

    public void reject(
            PaymentFailure rejection,
            Instant finalizedAt,
            PaymentPolicyBundle profiles
    ) {
        rejectInternal(
                rejection,
                finalizedAt,
                profiles,
                false
        );
    }

    public void rejectAfterPreConfirmationRevocation(
            PaymentFailure rejection,
            Instant finalizedAt,
            PaymentPolicyBundle profiles
    ) {
        rejectInternal(
                rejection,
                finalizedAt,
                profiles,
                true
        );
    }

    private void rejectInternal(
            PaymentFailure rejection,
            Instant finalizedAt,
            PaymentPolicyBundle profiles,
            boolean preConfirmationRevocationCompleted
    ) {
        Objects.requireNonNull(rejection, "Payment rejection");
        Objects.requireNonNull(finalizedAt, "Finalized instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (state.status() == PaymentStatus.REJECTED
                && state.failure().filter(rejection::equals).isPresent()) {
            return;
        }

        if (state.status() == PaymentStatus.PENDING_CONFIRMATION) {
            if (!preConfirmationRevocationCompleted) {
                throw PaymentDomainException.conflict(
                        "PENDING_CONFIRMATION rejection requires stable challenge revocation"
                );
            }
        } else {
            requireStatus("reject", PaymentStatus.RECEIVED);
        }

        requireRejectionFailure(rejection, "Payment rejection");

        PolicyDecision<FailureDispositionDecision> disposition =
                FAILURE_POLICY.decide(
                        rejection,
                        FinancialEffectKnowledge.PROVEN_NONE,
                        state.status(),
                        profiles.failureClassificationProfile()
                );
        requireDecision(
                disposition.decision()
                        == FailureDispositionDecision.BUSINESS_REJECT
                        || disposition.decision()
                        == FailureDispositionDecision.SECURITY_REJECT,
                disposition.reasonCode()
        );

        PaymentState next = nextBuilder(
                PaymentStatus.REJECTED,
                finalizedAt
        ).failure(rejection).build();

        PaymentStatus previous = state.status();
        EventBatch batch = new EventBatch(next, finalizedAt);
        commit(
                next,
                List.of(
                        rejected(batch, rejection, finalizedAt),
                        immediateResult(
                                batch,
                                previous,
                                next.status(),
                                rejection,
                                finalizedAt,
                                profiles
                        )
                )
        );
    }

    public void recordRecoverableFailure(
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(failure, "Recoverable failure");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (state.failure().filter(failure::equals).isPresent()) {
            return;
        }
        requireStatus(
                "recordRecoverableFailure",
                PaymentStatus.AUTHORIZATION_CHECKING,
                PaymentStatus.BANKING_VERIFICATION_PENDING,
                PaymentStatus.FUNDS_CONTROL_PENDING,
                PaymentStatus.TREASURY_ACCOUNT_RESOLUTION_PENDING
        );
        requireRecoverableFailure(failure, "Recoverable failure");

        PolicyDecision<FailureDispositionDecision> disposition =
                FAILURE_POLICY.decide(
                        failure,
                        FinancialEffectKnowledge.PROVEN_NONE,
                        state.status(),
                        profiles.failureClassificationProfile()
                );

        requireDecision(
                disposition.decision()
                        != FailureDispositionDecision.BUSINESS_REJECT
                        && disposition.decision()
                        != FailureDispositionDecision.SECURITY_REJECT
                        && disposition.decision()
                        != FailureDispositionDecision.REVERSAL_REQUIRED
                        && disposition.decision()
                        != FailureDispositionDecision.OUTCOME_UNKNOWN,
                disposition.reasonCode()
        );

        PaymentStatus retained = state.status();
        PaymentState next = nextBuilder(
                retained,
                decisionAt
        ).failure(failure).build();

        EventBatch batch = new EventBatch(next, decisionAt);
        commit(
                next,
                List.of(
                        processingDeferred(batch, failure, decisionAt),
                        immediateResult(
                                batch,
                                retained,
                                retained,
                                failure,
                                decisionAt,
                                profiles,
                                true
                        )
                )
        );
    }

    public void failWithoutFinancialEffect(
            PaymentFailure failure,
            Instant finalizedAt,
            PaymentPolicyBundle profiles
    ) {
        failWithoutFinancialEffectInternal(
                failure,
                finalizedAt,
                profiles,
                false
        );
    }

    public void failWithoutFinancialEffectAfterPreConfirmationRevocation(
            PaymentFailure failure,
            Instant finalizedAt,
            PaymentPolicyBundle profiles
    ) {
        failWithoutFinancialEffectInternal(
                failure,
                finalizedAt,
                profiles,
                true
        );
    }

    private void failWithoutFinancialEffectInternal(
            PaymentFailure failure,
            Instant finalizedAt,
            PaymentPolicyBundle profiles,
            boolean preConfirmationRevocationCompleted
    ) {
        Objects.requireNonNull(failure, "Technical failure");
        Objects.requireNonNull(finalizedAt, "Finalized instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (state.status() == PaymentStatus.FAILED
                && state.failure().filter(failure::equals).isPresent()) {
            return;
        }

        if (state.status() == PaymentStatus.PENDING_CONFIRMATION) {
            if (!preConfirmationRevocationCompleted) {
                throw PaymentDomainException.conflict(
                        "PENDING_CONFIRMATION failure requires stable challenge revocation"
                );
            }
        } else {
            requireStatus(
                    "failWithoutFinancialEffect",
                    PaymentStatus.RECEIVED,
                    PaymentStatus.AUTHORIZATION_CHECKING,
                    PaymentStatus.BANKING_VERIFICATION_PENDING,
                    PaymentStatus.FUNDS_CONTROL_PENDING,
                    PaymentStatus.TREASURY_ACCOUNT_RESOLUTION_PENDING,
                    PaymentStatus.APPROVED_FOR_POSTING
            );
        }
        if (failure.failureCategory()
                != FailureCategory.TECHNICAL_FAILURE) {
            throw PaymentDomainException.rejected(
                    "FAILED_REQUIRES_TECHNICAL_FAILURE"
            );
        }

        PolicyDecision<FailureDispositionDecision> disposition =
                FAILURE_POLICY.decide(
                        failure,
                        FinancialEffectKnowledge.PROVEN_NONE,
                        state.status(),
                        profiles.failureClassificationProfile()
                );
        requireDecision(
                disposition.decision()
                        == FailureDispositionDecision
                                .TECHNICAL_FAIL_NO_EFFECT,
                disposition.reasonCode()
        );

        PaymentStatus previous = state.status();
        PaymentState next = nextBuilder(
                PaymentStatus.FAILED,
                finalizedAt
        ).failure(failure).build();

        EventBatch batch = new EventBatch(next, finalizedAt);
        commit(
                next,
                List.of(
                        failedWithoutEffect(
                                batch,
                                failure,
                                finalizedAt
                        ),
                        immediateResult(
                                batch,
                                previous,
                                next.status(),
                                failure,
                                finalizedAt,
                                profiles
                        )
                )
        );
    }

    public PaymentState toState() {
        return state;
    }

    public List<PaymentDomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public PaymentId id() {
        return state.paymentId();
    }

    public PublicPaymentReference publicPaymentReference() {
        return state.publicPaymentReference();
    }

    public PaymentStatus status() {
        return state.status();
    }

    public long businessVersion() {
        return state.businessVersion();
    }

    private void applyPostingOutcome(
            PostingOutcomeSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles,
            boolean authoritativeResolution
    ) {
        Objects.requireNonNull(evidence, "Posting evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (samePostingEvidence(evidence)) {
            return;
        }

        if (authoritativeResolution) {
            requireStatus(
                    "resolvePostingOutcome",
                    PaymentStatus.POSTING_OUTCOME_UNKNOWN
            );
            if (evidence.metadata().observationChannel()
                    == EvidenceObservationChannel.DIRECT_RESPONSE) {
                throw PaymentDomainException.rejected(
                        "AUTHORITATIVE_LOOKUP_EVIDENCE_REQUIRED"
                );
            }
        } else {
            requireStatus(
                    "recordPostingOutcome",
                    PaymentStatus.POSTING_PENDING,
                    PaymentStatus.DEBIT_CONFIRMED
            );
        }

        PostingInstructionIdentity instruction =
                state.postingInstruction().orElseThrow();

        CurrentPostingEvidence current =
                state.postingOutcomeEvidence()
                        .map(existing -> new CurrentPostingEvidence(
                                evidenceIdentity(
                                        existing.postingInstructionId()
                                                + ":"
                                                + existing.metadata()
                                                .observationChannel()
                                                + ":"
                                                + existing.metadata()
                                                .acceptedAt(),
                                        existing.metadata()
                                                .evidenceFingerprint()
                                ),
                                authorityOf(
                                        existing.metadata()
                                                .observationChannel()
                                ),
                                postingConclusiveness(existing.outcome())
                        ))
                        .orElse(null);

        PostingDecisionInput input = new PostingDecisionInput(
                new PaymentPostingContext(
                        state.status(),
                        instruction.instructionId(),
                        instruction.idempotencyKey(),
                        state.requestedAmount()
                ),
                evidence,
                failure,
                current,
                authorityOf(evidence.metadata().observationChannel()),
                postingConclusiveness(evidence.outcome()),
                PaymentLifecycleContext.of(state.status()),
                decisionAt
        );

        PolicyDecision<PostingDecision> decision =
                POSTING_DECISION_SERVICE.decide(input, profiles);

        if (decision.decision() == PostingDecision.NO_OP) {
            return;
        }
        if (decision.decision() == PostingDecision.CONFLICT) {
            throw PaymentDomainException.conflict(
                    decision.reasonCode()
            );
        }
        if (authoritativeResolution
                && decision.decision()
                        == PostingDecision.POSTING_OUTCOME_UNKNOWN) {
            return;
        }

        BankPostingReference bankReference =
                mergeBankPostingReference(evidence);

        switch (decision.decision()) {
            case POSTED_PENDING_TFJ -> {
                PaymentState next = nextBuilder(
                        PaymentStatus.POSTED_PENDING_TFJ,
                        decisionAt
                ).postingOutcomeEvidence(evidence)
                        .bankPostingReference(bankReference)
                        .failure(null)
                        .build();

                EventBatch batch = new EventBatch(next, decisionAt);
                List<PaymentDomainEvent> events = new ArrayList<>();
                events.add(
                        authoritativeResolution
                                ? postingResolved(batch, evidence, decisionAt)
                                : postingRecorded(batch, evidence)
                );
                events.add(immediateResult(
                        batch,
                        state.status(),
                        next.status(),
                        null,
                        decisionAt,
                        profiles
                ));
                events.add(new PaymentEndOfDayTrackingRequested(
                        batch.metadata(),
                        next.financialInstitutionCode(),
                        requirePrincipalPostingReference(bankReference),
                        requireBusinessDate(evidence),
                        decisionAt
                ));
                commit(next, events);
            }
            case DEBIT_CONFIRMED -> {
                PaymentState next = nextBuilder(
                        PaymentStatus.DEBIT_CONFIRMED,
                        decisionAt
                ).postingOutcomeEvidence(evidence)
                        .bankPostingReference(bankReference)
                        .failure(failure)
                        .build();

                EventBatch batch = new EventBatch(next, decisionAt);
                List<PaymentDomainEvent> events = new ArrayList<>();
                events.add(
                        authoritativeResolution
                                ? postingResolved(batch, evidence, decisionAt)
                                : postingRecorded(batch, evidence)
                );
                events.add(new PaymentDebitConfirmed(
                        batch.metadata(),
                        evidence.postingInstructionId(),
                        requirePrincipalPostingReference(bankReference),
                        evidence.debitLeg()
                                .bankEntryReferenceOptional()
                                .orElse(null),
                        evidence.businessDate().orElse(null),
                        evidence.debitLeg()
                                .effectiveAtOptional()
                                .orElse(null)
                ));
                events.add(immediateResult(
                        batch,
                        state.status(),
                        next.status(),
                        failure,
                        decisionAt,
                        profiles,
                        true
                ));
                commit(next, events);
            }
            case POSTING_OUTCOME_UNKNOWN -> {
                PaymentFailure uncertain = requireUncertainFailure(
                        failure,
                        "Unknown posting outcome"
                );
                PaymentState next = nextBuilder(
                        PaymentStatus.POSTING_OUTCOME_UNKNOWN,
                        decisionAt
                ).postingOutcomeEvidence(evidence)
                        .bankPostingReference(bankReference)
                        .failure(uncertain)
                        .build();

                EventBatch batch = new EventBatch(next, decisionAt);
                commit(
                        next,
                        List.of(
                                postingRecorded(batch, evidence),
                                new PaymentPostingOutcomeLookupRequested(
                                        batch.metadata(),
                                        evidence.postingInstructionId(),
                                        evidence
                                                .postingCommandIdempotencyKey(),
                                        bankReference == null
                                                ? null
                                                : bankReference
                                                .principalPostingReference(),
                                        lookupMode(evidence),
                                        evidence.metadata().acceptedAt(),
                                        decisionAt
                                ),
                                immediateResult(
                                        batch,
                                        state.status(),
                                        next.status(),
                                        uncertain,
                                        decisionAt,
                                        profiles,
                                        true
                                )
                        )
                );
            }
            case REJECTED_NO_EFFECT -> {
                PaymentFailure rejection = requireRejectionFailure(
                        failure,
                        "Posting business rejection"
                );
                PaymentState next = nextBuilder(
                        PaymentStatus.REJECTED,
                        decisionAt
                ).postingOutcomeEvidence(evidence)
                        .bankPostingReference(bankReference)
                        .failure(rejection)
                        .build();

                EventBatch batch = new EventBatch(next, decisionAt);
                commit(
                        next,
                        List.of(
                                authoritativeResolution
                                        ? postingResolved(
                                                batch,
                                                evidence,
                                                decisionAt
                                        )
                                        : postingRecorded(
                                                batch,
                                                evidence
                                        ),
                                rejected(batch, rejection, decisionAt),
                                immediateResult(
                                        batch,
                                        state.status(),
                                        next.status(),
                                        rejection,
                                        decisionAt,
                                        profiles
                                )
                        )
                );
            }
            case FAILED_NO_EFFECT -> {
                PaymentFailure technical = requireTechnicalFailure(
                        failure,
                        "Posting technical failure"
                );
                PaymentState next = nextBuilder(
                        PaymentStatus.FAILED,
                        decisionAt
                ).postingOutcomeEvidence(evidence)
                        .bankPostingReference(bankReference)
                        .failure(technical)
                        .build();

                EventBatch batch = new EventBatch(next, decisionAt);
                commit(
                        next,
                        List.of(
                                authoritativeResolution
                                        ? postingResolved(
                                                batch,
                                                evidence,
                                                decisionAt
                                        )
                                        : postingRecorded(
                                                batch,
                                                evidence
                                        ),
                                failedWithoutEffect(
                                        batch,
                                        technical,
                                        decisionAt
                                ),
                                immediateResult(
                                        batch,
                                        state.status(),
                                        next.status(),
                                        technical,
                                        decisionAt,
                                        profiles
                                )
                        )
                );
            }
            case REVERSAL_REQUIRED -> {
                PaymentFailure reversalFailure =
                        requireFinancialEffectFailure(
                                failure,
                                evidence
                        );
                PaymentState next = nextBuilder(
                        PaymentStatus.REVERSAL_REQUIRED,
                        decisionAt
                ).postingOutcomeEvidence(evidence)
                        .bankPostingReference(
                                Objects.requireNonNull(
                                        bankReference,
                                        "Bank posting reference"
                                )
                        )
                        .failure(reversalFailure)
                        .build();

                EventBatch batch = new EventBatch(next, decisionAt);
                commit(
                        next,
                        List.of(
                                authoritativeResolution
                                        ? postingResolved(
                                                batch,
                                                evidence,
                                                decisionAt
                                        )
                                        : postingRecorded(
                                                batch,
                                                evidence
                                        ),
                                reversalRequired(
                                        batch,
                                        reversalFailure.failureCode(),
                                        ReversalSourceStage.POSTING,
                                        decisionAt
                                ),
                                immediateResult(
                                        batch,
                                        state.status(),
                                        next.status(),
                                        reversalFailure,
                                        decisionAt,
                                        profiles
                                )
                        )
                );
            }
            case NO_OP, CONFLICT -> throw new IllegalStateException(
                    "Posting service terminal decision was already handled"
            );
        }
    }

    private void applyReversalOutcome(
            ReversalSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle profiles,
            boolean authoritativeResolution
    ) {
        Objects.requireNonNull(evidence, "Reversal evidence");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profiles, "Policy bundle");

        if (sameReversalEvidence(evidence)) {
            return;
        }

        if (authoritativeResolution) {
            requireStatus(
                    "resolveReversalOutcome",
                    PaymentStatus.REVERSAL_OUTCOME_UNKNOWN
            );
            if (evidence.outcome().orElseThrow()
                    .metadata().observationChannel()
                    == EvidenceObservationChannel.DIRECT_RESPONSE) {
                throw PaymentDomainException.rejected(
                        "AUTHORITATIVE_REVERSAL_LOOKUP_REQUIRED"
                );
            }
        } else {
            requireStatus(
                    "recordReversalOutcome",
                    PaymentStatus.REVERSAL_PENDING
            );
        }

        ReversalInstructionIdentity instruction =
                state.reversalInstruction().orElseThrow();

        CurrentReversalEvidence current = state.reversalEvidence()
                .filter(existing -> existing.outcome().isPresent())
                .map(existing -> new CurrentReversalEvidence(
                        evidenceIdentity(
                                existing.reversalInstructionId()
                                        + ":"
                                        + existing.outcome()
                                        .orElseThrow()
                                        .metadata()
                                        .observationChannel()
                                        + ":"
                                        + existing.outcome()
                                        .orElseThrow()
                                        .metadata()
                                        .acceptedAt(),
                                existing.outcome()
                                        .orElseThrow()
                                        .metadata()
                                        .evidenceFingerprint()
                        ),
                        authorityOf(
                                existing.outcome()
                                        .orElseThrow()
                                        .metadata()
                                        .observationChannel()
                        ),
                        reversalConclusiveness(
                                existing.outcome()
                                        .orElseThrow()
                                        .outcome()
                        )
                ))
                .orElse(null);

        ReversalDecisionInput input = new ReversalDecisionInput(
                null,
                new PaymentReversalContext(
                        state.status(),
                        instruction.instructionId(),
                        instruction.idempotencyKey(),
                        FinancialEffectKnowledge.CONFIRMED_PARTIAL
                ),
                instruction,
                state.reversalAuthorizationEvidence().orElseThrow(),
                evidence,
                failure,
                current,
                authorityOf(
                        evidence.outcome()
                                .orElseThrow()
                                .metadata()
                                .observationChannel()
                ),
                reversalConclusiveness(
                        evidence.outcome().orElseThrow().outcome()
                ),
                PaymentLifecycleContext.of(state.status()),
                decisionAt
        );

        PolicyDecision<ReversalDecision> decision =
                REVERSAL_DECISION_SERVICE.decide(input, profiles);

        if (decision.decision() == ReversalDecision.NO_OP) {
            return;
        }
        if (decision.decision() == ReversalDecision.CONFLICT) {
            throw PaymentDomainException.conflict(
                    decision.reasonCode()
            );
        }
        if (authoritativeResolution
                && decision.decision()
                        == ReversalDecision.REVERSAL_OUTCOME_UNKNOWN) {
            return;
        }

        ReversalOutcomeEvidence outcome =
                evidence.outcome().orElseThrow();

        if (decision.decision() == ReversalDecision.REVERSED) {
            ReversalReference reversalReference =
                    outcome.reversalReference().orElseThrow();

            PaymentState next = nextBuilder(
                    PaymentStatus.REVERSED,
                    decisionAt
            ).reversalEvidence(evidence)
                    .failure(null)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            authoritativeResolution
                                    ? reversalResolved(
                                            batch,
                                            evidence,
                                            decisionAt
                                    )
                                    : reversalRecorded(batch, evidence),
                            new PaymentReversed(
                                    batch.metadata(),
                                    evidence.reversalInstructionId(),
                                    requirePrincipalPostingReference(),
                                    reversalReference,
                                    decisionAt
                            ),
                            reversalResult(
                                    batch,
                                    PaymentStatus.REVERSAL_PENDING,
                                    next.status(),
                                    null,
                                    reversalReference,
                                    decisionAt,
                                    profiles
                            )
                    )
            );
            return;
        }

        if (decision.decision()
                == ReversalDecision.REVERSAL_OUTCOME_UNKNOWN) {
            PaymentFailure uncertain = requireUncertainFailure(
                    failure,
                    "Unknown reversal outcome"
            );
            PaymentState next = nextBuilder(
                    PaymentStatus.REVERSAL_OUTCOME_UNKNOWN,
                    decisionAt
            ).reversalEvidence(evidence)
                    .failure(uncertain)
                    .build();

            EventBatch batch = new EventBatch(next, decisionAt);
            commit(
                    next,
                    List.of(
                            reversalRecorded(batch, evidence),
                            new PaymentReversalOutcomeLookupRequested(
                                    batch.metadata(),
                                    evidence.reversalInstructionId(),
                                    evidence.reversalCommandIdempotencyKey(),
                                    outcome.reversalReference()
                                            .orElse(null),
                                    decisionAt
                            )
                    )
            );
            return;
        }

        PaymentFailure required = requireReversalFailure(
                failure,
                outcome
        );
        PaymentState next = nextBuilder(
                PaymentStatus.REVERSAL_REQUIRED,
                decisionAt
        ).reversalEvidence(evidence)
                .failure(required)
                .build();

        EventBatch batch = new EventBatch(next, decisionAt);
        commit(
                next,
                List.of(
                        authoritativeResolution
                                ? reversalResolved(
                                        batch,
                                        evidence,
                                        decisionAt
                                )
                                : reversalRecorded(batch, evidence),
                        reversalRequired(
                                batch,
                                required.failureCode(),
                                ReversalSourceStage.REVERSAL,
                                decisionAt
                        ),
                        reversalResult(
                                batch,
                                state.status(),
                                next.status(),
                                required,
                                outcome.reversalReference()
                                        .orElse(null),
                                decisionAt,
                                profiles
                        )
                )
        );
    }

    private PaymentState.Builder nextBuilder(
            PaymentStatus targetStatus,
            Instant occurredAt
    ) {
        requireMonotonicTime(occurredAt);
        return state.toBuilder()
                .status(targetStatus)
                .businessVersion(state.businessVersion() + 1)
                .updatedAt(occurredAt)
                .finalizedAt(
                        targetStatus.isTerminal()
                                ? occurredAt
                                : null
                );
    }

    private void commit(
            PaymentState next,
            List<? extends PaymentDomainEvent> events
    ) {
        Objects.requireNonNull(next, "Next Payment state");
        Objects.requireNonNull(events, "Domain events");
        if (events.isEmpty()) {
            throw new IllegalArgumentException(
                    "A successful Payment mutation requires domain events"
            );
        }

        for (int index = 0; index < events.size(); index++) {
            PaymentDomainEvent event = Objects.requireNonNull(
                    events.get(index),
                    "Domain event"
            );
            if (!event.paymentId().equals(next.paymentId())
                    || !event.paymentReference().equals(
                            next.publicPaymentReference()
                    )
                    || event.aggregateVersion()
                            != next.businessVersion()
                    || event.eventSequence() != index + 1
                    || event.paymentStatus() != next.status()) {
                throw new IllegalArgumentException(
                        "Domain event metadata is inconsistent with mutation"
                );
            }
        }

        state = next;
        domainEvents.addAll(events);
    }

    private EventBatch eventBatch(
            PaymentState next,
            Instant occurredAt
    ) {
        return new EventBatch(next, occurredAt);
    }


    private PaymentBankingVerificationRecorded bankingRecorded(
            EventBatch batch,
            BankingVerificationSnapshot evidence
    ) {
        List<SafeCheckResult> checks = evidence.checks().stream()
                .map(check -> new SafeCheckResult(
                        check.type().name(),
                        check.result(),
                        check.reasonCodeOptional().orElse(null)
                ))
                .toList();

        return new PaymentBankingVerificationRecorded(
                batch.metadata(),
                evidence.verificationId(),
                evidence.outcome(),
                checks,
                evidence.metadata().evidenceFingerprint(),
                evidence.metadata().acceptedAt()
        );
    }

    private PaymentFundsControlRecorded fundsRecorded(
            EventBatch batch,
            FundsControlSnapshot evidence
    ) {
        List<SafeCheckResult> checks = evidence.checks().stream()
                .map(check -> new SafeCheckResult(
                        check.type().name(),
                        check.result(),
                        check.reasonCodeOptional().orElse(null)
                ))
                .toList();

        return new PaymentFundsControlRecorded(
                batch.metadata(),
                evidence.verificationReference(),
                evidence.outcome(),
                checks,
                evidence.validUntil(),
                evidence.metadata().evidenceFingerprint(),
                evidence.metadata().acceptedAt()
        );
    }

    private PaymentTreasuryAccountResolutionRecorded treasuryRecorded(
            EventBatch batch,
            TreasuryAccountResolutionSnapshot evidence,
            TreasuryAccountReference account,
            PaymentFailure failure
    ) {
        return new PaymentTreasuryAccountResolutionRecorded(
                batch.metadata(),
                evidence.resolutionOutcome(),
                account == null
                        ? null
                        : account.treasuryConfigurationId(),
                account == null
                        ? null
                        : account.configurationVersion(),
                account == null
                        ? null
                        : account.maskedDisplay(),
                evidence.rejectionCode()
                        .orElse(
                                failure == null
                                        ? null
                                        : failure.failureCode()
                        ),
                evidence.metadata().evidenceFingerprint(),
                evidence.metadata().acceptedAt()
        );
    }

    private PaymentPostingOutcomeRecorded postingRecorded(
            EventBatch batch,
            PostingOutcomeSnapshot evidence
    ) {
        return new PaymentPostingOutcomeRecorded(
                batch.metadata(),
                evidence.postingInstructionId(),
                evidence.outcome(),
                evidence.bankPostingReference()
                        .map(
                                BankPostingReference
                                        ::principalPostingReference
                        )
                        .orElse(null),
                PostingLegPayload.from(evidence.debitLeg()),
                PostingLegPayload.from(evidence.cutCreditLeg()),
                evidence.businessDate().orElse(null),
                evidence.rejectionCode().orElse(null),
                evidence.nextAction(),
                evidence.metadata().evidenceFingerprint(),
                evidence.metadata().acceptedAt()
        );
    }

    private PaymentPostingOutcomeResolved postingResolved(
            EventBatch batch,
            PostingOutcomeSnapshot evidence,
            Instant resolvedAt
    ) {
        return new PaymentPostingOutcomeResolved(
                batch.metadata(),
                evidence.postingInstructionId(),
                PostingOutcome.UNKNOWN,
                evidence.outcome(),
                evidence.bankPostingReference()
                        .map(
                                BankPostingReference
                                        ::principalPostingReference
                        )
                        .orElse(null),
                PostingLegPayload.from(evidence.debitLeg()),
                PostingLegPayload.from(evidence.cutCreditLeg()),
                evidence.businessDate().orElse(null),
                evidence.rejectionCode().orElse(null),
                evidence.metadata().evidenceFingerprint(),
                resolvedAt
        );
    }

    private PaymentEndOfDayConfirmationRecorded tfjRecorded(
            EventBatch batch,
            EndOfDayConfirmationSnapshot evidence
    ) {
        TfjFailureEvidence failure =
                evidence.failureEvidence().orElse(null);

        return new PaymentEndOfDayConfirmationRecorded(
                batch.metadata(),
                evidence.confirmationId(),
                evidence.financialInstitutionCode(),
                evidence.businessDate(),
                evidence.principalBankPostingReference(),
                evidence.tfjStatus(),
                failure == null ? null : failure.code(),
                failure == null
                        ? null
                        : failure.recoveryAction(),
                evidence.confirmedAt(),
                evidence.matchedAt(),
                evidence.metadata().evidenceFingerprint()
        );
    }

    private PaymentReversalOutcomeRecorded reversalRecorded(
            EventBatch batch,
            ReversalSnapshot evidence
    ) {
        ReversalOutcomeEvidence outcome =
                evidence.outcome().orElseThrow();

        return new PaymentReversalOutcomeRecorded(
                batch.metadata(),
                evidence.reversalInstructionId(),
                outcome.outcome(),
                outcome.reversalReference().orElse(null),
                outcome.reversalEntryReference().orElse(null),
                outcome.reasonCode().orElse(null),
                outcome.metadata().evidenceFingerprint(),
                outcome.metadata().acceptedAt()
        );
    }

    private PaymentReversalOutcomeResolved reversalResolved(
            EventBatch batch,
            ReversalSnapshot evidence,
            Instant resolvedAt
    ) {
        ReversalOutcomeEvidence outcome =
                evidence.outcome().orElseThrow();

        return new PaymentReversalOutcomeResolved(
                batch.metadata(),
                evidence.reversalInstructionId(),
                ReversalOutcome.UNKNOWN,
                outcome.outcome(),
                outcome.reversalReference().orElse(null),
                outcome.reversalEntryReference().orElse(null),
                outcome.reasonCode().orElse(null),
                outcome.metadata().evidenceFingerprint(),
                resolvedAt
        );
    }

    private PaymentRejected rejected(
            EventBatch batch,
            PaymentFailure failure,
            Instant finalizedAt
    ) {
        return new PaymentRejected(
                batch.metadata(),
                failure.failureCode(),
                failure.failureCategory(),
                failure.failureStage(),
                finalizedAt
        );
    }

    private PaymentFailedWithoutFinancialEffect failedWithoutEffect(
            EventBatch batch,
            PaymentFailure failure,
            Instant finalizedAt
    ) {
        return new PaymentFailedWithoutFinancialEffect(
                batch.metadata(),
                failure.failureCode(),
                failure.failureCategory(),
                failure.failureStage(),
                finalizedAt
        );
    }

    private PaymentProcessingDeferred processingDeferred(
            EventBatch batch,
            PaymentFailure failure,
            Instant deferredAt
    ) {
        return new PaymentProcessingDeferred(
                batch.metadata(),
                failure.failureCode(),
                failure.failureCategory(),
                failure.failureStage(),
                failure.retryDisposition(),
                deferredAt
        );
    }

    private PaymentReversalRequired reversalRequired(
            EventBatch batch,
            FailureCode reasonCode,
            ReversalSourceStage sourceStage,
            Instant requiredAt
    ) {
        PostingLegStatus debitStatus = batch.state
                .postingOutcomeEvidence()
                .map(snapshot -> snapshot.debitLeg().status())
                .orElse(PostingLegStatus.UNKNOWN);
        PostingLegStatus cutStatus = batch.state
                .postingOutcomeEvidence()
                .map(snapshot -> snapshot.cutCreditLeg().status())
                .orElse(PostingLegStatus.UNKNOWN);

        String principal = batch.state.bankPostingReference()
                .map(BankPostingReference::principalPostingReference)
                .orElseThrow(() -> PaymentDomainException.rejected(
                        "PRINCIPAL_POSTING_REFERENCE_REQUIRED"
                ));

        return new PaymentReversalRequired(
                batch.metadata(),
                principal,
                reasonCode,
                sourceStage,
                debitStatus,
                cutStatus,
                requiredAt
        );
    }

    private PaymentImmediateResultAvailable immediateResult(
            EventBatch batch,
            PaymentStatus previousStatus,
            PaymentStatus resultingStatus,
            PaymentFailure failure,
            Instant availableAt,
            PaymentPolicyBundle profiles
    ) {
        return immediateResult(
                batch,
                previousStatus,
                resultingStatus,
                failure,
                availableAt,
                profiles,
                false
        );
    }

    private PaymentImmediateResultAvailable immediateResult(
            EventBatch batch,
            PaymentStatus previousStatus,
            PaymentStatus resultingStatus,
            PaymentFailure failure,
            Instant availableAt,
            PaymentPolicyBundle profiles,
            boolean forceProcessingForSameStatus
    ) {
        PolicyDecision<ResultIntentDecision> decision =
                RESULT_INTENT_SERVICE.decide(
                        new PaymentResultContext(
                                batch.state.publicPaymentReference(),
                                batch.state.requestIdentity()
                                        .correlationId()
                                        .value()
                        ),
                        previousStatus,
                        resultingStatus,
                        failure,
                        availableAt,
                        profiles
                );

        ResultIntentDecision selected = decision.decision();
        if (forceProcessingForSameStatus
                && selected == ResultIntentDecision.NONE) {
            selected = ResultIntentDecision.IMMEDIATE_PROCESSING;
        }

        PaymentImmediateResultType resultType = switch (selected) {
            case IMMEDIATE_PROCESSING ->
                    PaymentImmediateResultType.PROCESSING;
            case IMMEDIATE_REJECTED ->
                    PaymentImmediateResultType.REJECTED;
            case IMMEDIATE_FAILED ->
                    PaymentImmediateResultType.FAILED;
            case IMMEDIATE_POSTED_PENDING_TFJ ->
                    PaymentImmediateResultType.POSTED_PENDING_TFJ;
            case IMMEDIATE_REVERSAL_REQUIRED ->
                    PaymentImmediateResultType.REVERSAL_REQUIRED;
            default -> throw PaymentDomainException.rejected(
                    "IMMEDIATE_RESULT_PROFILE_MISSING_FOR_"
                            + resultingStatus
            );
        };

        String principal = batch.state.bankPostingReference()
                .map(BankPostingReference::principalPostingReference)
                .orElse(null);
        LocalDate businessDate = batch.state.postingOutcomeEvidence()
                .flatMap(PostingOutcomeSnapshot::businessDate)
                .orElse(null);

        return new PaymentImmediateResultAvailable(
                batch.metadata(),
                batch.state.externalPaymentReference(),
                resultType,
                failure == null ? null : failure.failureCode(),
                principal,
                businessDate,
                availableAt
        );
    }

    private PaymentReversalResultAvailable reversalResult(
            EventBatch batch,
            PaymentStatus previousStatus,
            PaymentStatus resultingStatus,
            PaymentFailure failure,
            ReversalReference reversalReference,
            Instant availableAt,
            PaymentPolicyBundle profiles
    ) {
        PolicyDecision<ResultIntentDecision> decision =
                RESULT_INTENT_SERVICE.decide(
                        new PaymentResultContext(
                                batch.state.publicPaymentReference(),
                                batch.state.requestIdentity()
                                        .correlationId()
                                        .value()
                        ),
                        previousStatus,
                        resultingStatus,
                        failure,
                        availableAt,
                        profiles
                );

        PaymentReversalResultType resultType =
                switch (decision.decision()) {
                    case REVERSAL_REVERSED ->
                            PaymentReversalResultType.REVERSED;
                    case REVERSAL_REJECTED_OR_NOT_ALLOWED,
                            IMMEDIATE_REVERSAL_REQUIRED ->
                            PaymentReversalResultType.REVERSAL_REQUIRED;
                    default -> throw PaymentDomainException.rejected(
                            "REVERSAL_RESULT_PROFILE_MISSING_FOR_"
                                    + resultingStatus
                    );
                };

        return new PaymentReversalResultAvailable(
                batch.metadata(),
                batch.state.externalPaymentReference(),
                resultType,
                batch.state.bankPostingReference()
                        .map(
                                BankPostingReference
                                        ::principalPostingReference
                        )
                        .orElseThrow(() ->
                                PaymentDomainException.rejected(
                                        "PRINCIPAL_POSTING_REFERENCE_REQUIRED"
                                )
                        ),
                reversalReference,
                failure == null ? null : failure.failureCode(),
                availableAt
        );
    }

    private void requireStatus(
            String operation,
            PaymentStatus... allowed
    ) {
        for (PaymentStatus candidate : allowed) {
            if (state.status() == candidate) {
                return;
            }
        }
        throw PaymentDomainException.invalidTransition(
                state.status(),
                operation
        );
    }

    private void requireMonotonicTime(Instant instant) {
        Objects.requireNonNull(instant, "Operation instant");
        if (instant.isBefore(state.updatedAt())) {
            throw PaymentDomainException.rejected(
                    "OPERATION_TIME_PRECEDES_UPDATED_AT"
            );
        }
    }

    private static void requireDecision(
            boolean condition,
            String reasonCode
    ) {
        if (!condition) {
            throw PaymentDomainException.rejected(reasonCode);
        }
    }

    private PaymentFailure requireRejectionFailure(
            PaymentFailure failure,
            String label
    ) {
        Objects.requireNonNull(failure, label + " failure");
        if (failure.failureCategory()
                != FailureCategory.BUSINESS_REJECTION
                && failure.failureCategory()
                != FailureCategory.SECURITY_REJECTION) {
            throw PaymentDomainException.rejected(
                    label + " requires business or security category"
            );
        }
        return failure;
    }

    private PaymentFailure requireTechnicalFailure(
            PaymentFailure failure,
            String label
    ) {
        Objects.requireNonNull(failure, label + " failure");
        if (failure.failureCategory()
                != FailureCategory.TECHNICAL_FAILURE) {
            throw PaymentDomainException.rejected(
                    label + " requires technical category"
            );
        }
        return failure;
    }

    private PaymentFailure requireRecoverableFailure(
            PaymentFailure failure,
            String label
    ) {
        Objects.requireNonNull(failure, label + " failure");
        if (failure.retryDisposition()
                == RetryDisposition.NOT_RETRYABLE
                || failure.retryDisposition()
                == RetryDisposition.AUTHORITATIVE_LOOKUP_REQUIRED) {
            throw PaymentDomainException.rejected(
                    label + " is not recoverable input this operation"
            );
        }
        return failure;
    }

    private PaymentFailure requireUncertainFailure(
            PaymentFailure failure,
            String label
    ) {
        Objects.requireNonNull(failure, label + " failure");
        if (failure.failureCategory()
                != FailureCategory.UNCERTAIN_EXTERNAL_OUTCOME
                || failure.retryDisposition()
                != RetryDisposition.AUTHORITATIVE_LOOKUP_REQUIRED) {
            throw PaymentDomainException.rejected(
                    label + " requires authoritative lookup classification"
            );
        }
        return failure;
    }

    private PaymentFailure requireFinancialEffectFailure(
            PaymentFailure failure,
            PostingOutcomeSnapshot evidence
    ) {
        Objects.requireNonNull(
                failure,
                "Reversal-required posting failure"
        );
        if (evidence.bankPostingReference().isEmpty()) {
            throw PaymentDomainException.rejected(
                    "REVERSAL_REQUIRES_POSTING_REFERENCE"
            );
        }
        return failure;
    }

    private PaymentFailure requireTfjFailure(
            PaymentFailure failure,
            EndOfDayConfirmationSnapshot evidence
    ) {
        Objects.requireNonNull(failure, "TFJ reconciliation failure");
        TfjFailureEvidence tfjFailure =
                evidence.failureEvidence().orElseThrow();
        if (!failure.failureCode().equals(tfjFailure.code())
                || failure.failureCategory()
                        != FailureCategory
                                .TREASURY_RECONCILIATION_FAILURE) {
            throw PaymentDomainException.rejected(
                    "TFJ_FAILURE_CLASSIFICATION_MISMATCH"
            );
        }
        return failure;
    }

    private PaymentFailure requireReversalFailure(
            PaymentFailure failure,
            ReversalOutcomeEvidence outcome
    ) {
        Objects.requireNonNull(failure, "Reversal outcome failure");
        FailureCode reason = outcome.reasonCode().orElseThrow();
        if (!failure.failureCode().equals(reason)) {
            throw PaymentDomainException.rejected(
                    "REVERSAL_FAILURE_CODE_MISMATCH"
            );
        }
        return failure;
    }


    private boolean sameBankingEvidence(
            BankingVerificationSnapshot candidate
    ) {
        return state.bankingVerificationEvidence()
                .map(current ->
                        current.verificationId().equals(
                                candidate.verificationId()
                        ) && current.metadata()
                                .evidenceFingerprint()
                                .equals(
                                        candidate.metadata()
                                                .evidenceFingerprint()
                                )
                )
                .orElse(false);
    }

    private boolean sameFundsEvidence(
            FundsControlSnapshot candidate
    ) {
        return state.fundsControlEvidence()
                .map(current ->
                        current.verificationReference().equals(
                                candidate.verificationReference()
                        ) && current.metadata()
                                .evidenceFingerprint()
                                .equals(
                                        candidate.metadata()
                                                .evidenceFingerprint()
                                )
                )
                .orElse(false);
    }

    private boolean sameTreasuryEvidence(
            TreasuryAccountResolutionSnapshot candidate
    ) {
        return state.treasuryResolutionEvidence()
                .map(current ->
                        current.allocationIntentFingerprint().equals(
                                candidate.allocationIntentFingerprint()
                        ) && current.metadata()
                                .evidenceFingerprint()
                                .equals(
                                        candidate.metadata()
                                                .evidenceFingerprint()
                                )
                )
                .orElse(false);
    }

    private boolean samePostingEvidence(
            PostingOutcomeSnapshot candidate
    ) {
        return state.postingOutcomeEvidence()
                .map(current ->
                        current.postingInstructionId().equals(
                                candidate.postingInstructionId()
                        ) && current.metadata()
                                .evidenceFingerprint()
                                .equals(
                                        candidate.metadata()
                                                .evidenceFingerprint()
                                )
                )
                .orElse(false);
    }

    private boolean sameTfjEvidence(
            EndOfDayConfirmationSnapshot candidate
    ) {
        return state.endOfDayConfirmationEvidence()
                .map(current ->
                        current.confirmationId().equals(
                                candidate.confirmationId()
                        ) && current.metadata()
                                .evidenceFingerprint()
                                .equals(
                                        candidate.metadata()
                                                .evidenceFingerprint()
                                )
                )
                .orElse(false);
    }

    private boolean sameReversalEvidence(
            ReversalSnapshot candidate
    ) {
        if (candidate.outcome().isEmpty()) {
            return false;
        }
        return state.reversalEvidence()
                .filter(current -> current.outcome().isPresent())
                .map(current ->
                        current.reversalInstructionId().equals(
                                candidate.reversalInstructionId()
                        ) && current.outcome()
                                .orElseThrow()
                                .metadata()
                                .evidenceFingerprint()
                                .equals(
                                        candidate.outcome()
                                                .orElseThrow()
                                                .metadata()
                                                .evidenceFingerprint()
                                )
                )
                .orElse(false);
    }

    private BankPostingReference mergeBankPostingReference(
            PostingOutcomeSnapshot evidence
    ) {
        BankPostingReference current =
                state.bankPostingReference().orElse(null);
        BankPostingReference candidate =
                evidence.bankPostingReference().orElse(null);

        if (current != null && candidate != null
                && !current.equals(candidate)) {
            throw PaymentDomainException.conflict(
                    "Original bank posting reference cannot change"
            );
        }
        return current == null ? candidate : current;
    }

    private static EvidenceAuthority authorityOf(
            EvidenceObservationChannel channel
    ) {
        return switch (channel) {
            case DIRECT_RESPONSE, LOCAL_VALIDATION,
                    PROTECTED_CONFIGURATION_RESOLUTION ->
                    EvidenceAuthority.DIRECT_RESPONSE;
            case IDEMPOTENCY_LOOKUP ->
                    EvidenceAuthority.IDEMPOTENCY_LOOKUP;
            case BANK_REFERENCE_LOOKUP,
                    ASYNC_CALLBACK,
                    SCHEDULED_LOOKUP ->
                    EvidenceAuthority.BANK_REFERENCE_LOOKUP;
        };
    }

    private static EvidenceConclusiveness postingConclusiveness(
            PostingOutcome outcome
    ) {
        return switch (outcome) {
            case UNKNOWN -> EvidenceConclusiveness.INDETERMINATE;
            case DEBIT_CONFIRMED_CUT_CREDIT_PENDING ->
                    EvidenceConclusiveness.PARTIAL;
            case COMPLETED, REJECTED_NO_FINANCIAL_EFFECT,
                    REVERSAL_REQUIRED ->
                    EvidenceConclusiveness.CONCLUSIVE;
        };
    }

    private static EvidenceConclusiveness reversalConclusiveness(
            ReversalOutcome outcome
    ) {
        return outcome == ReversalOutcome.UNKNOWN
                ? EvidenceConclusiveness.INDETERMINATE
                : EvidenceConclusiveness.CONCLUSIVE;
    }

    private static PostingLookupMode lookupMode(
            PostingOutcomeSnapshot evidence
    ) {
        if (evidence.metadata().observationChannel()
                == EvidenceObservationChannel.BANK_REFERENCE_LOOKUP
                || evidence.bankPostingReference().isPresent()) {
            return PostingLookupMode.BANK_REFERENCE;
        }
        return PostingLookupMode.IDEMPOTENCY_KEY;
    }

    private static EvidenceIdentity evidenceIdentity(
            String identity,
            EvidenceFingerprint fingerprint
    ) {
        return new EvidenceIdentity(identity, fingerprint);
    }

    private String requirePrincipalPostingReference() {
        return state.bankPostingReference()
                .map(BankPostingReference::principalPostingReference)
                .orElseThrow(() -> PaymentDomainException.rejected(
                        "PRINCIPAL_POSTING_REFERENCE_REQUIRED"
                ));
    }

    private static String requirePrincipalPostingReference(
            BankPostingReference reference
    ) {
        if (reference == null) {
            throw PaymentDomainException.rejected(
                    "PRINCIPAL_POSTING_REFERENCE_REQUIRED"
            );
        }
        return reference.principalPostingReference();
    }

    private LocalDate requireBusinessDate() {
        return state.postingOutcomeEvidence()
                .flatMap(PostingOutcomeSnapshot::businessDate)
                .orElseThrow(() -> PaymentDomainException.rejected(
                        "POSTING_BUSINESS_DATE_REQUIRED"
                ));
    }

    private static LocalDate requireBusinessDate(
            PostingOutcomeSnapshot evidence
    ) {
        return evidence.businessDate()
                .orElseThrow(() -> PaymentDomainException.rejected(
                        "POSTING_BUSINESS_DATE_REQUIRED"
                ));
    }

    private static final class EventBatch {

        private final PaymentState state;
        private final Instant occurredAt;
        private int sequence;

        private EventBatch(
                PaymentState state,
                Instant occurredAt
        ) {
            this.state = Objects.requireNonNull(state, "Payment state");
            this.occurredAt = Objects.requireNonNull(
                    occurredAt,
                    "Event occurrence instant"
            );
        }

        private PaymentEventMetadata metadata() {
            sequence++;
            return new PaymentEventMetadata(
                    UUID.randomUUID(),
                    state.paymentId(),
                    state.publicPaymentReference(),
                    state.requestIdentity().correlationId(),
                    state.status(),
                    state.businessVersion(),
                    sequence,
                    null,
                    occurredAt
            );
        }
    }
}
