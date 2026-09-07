package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.PaymentTreasuryAccountResolutionPort;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import com.sixpay.payment.domain.model.evidence.TreasuryAccountResolutionSnapshot;
import com.sixpay.payment.domain.model.evidence.TreasuryResolutionOutcome;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

@Service
public final class PaymentT0OrchestrationService {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentFundsControlService fundsControlService;
    private final PaymentTreasuryResolutionService treasuryResolutionService;
    private final ObjectProvider<PaymentTreasuryAccountResolutionPort>
            treasuryPortProvider;
    private final PaymentT0IdentityFactory identityFactory;
    private final PaymentT0SnapshotService snapshotService;
    private final ObjectProvider<PaymentEventLifecycleOrchestrationService>
            lifecycleProvider;
    private final PaymentPolicyBundle policies;
    private final TimeProvider timeProvider;

    public PaymentT0OrchestrationService(
            PaymentLookupPort paymentLookupPort,
            PaymentFundsControlService fundsControlService,
            PaymentTreasuryResolutionService treasuryResolutionService,
            ObjectProvider<PaymentTreasuryAccountResolutionPort>
                    treasuryPortProvider,
            PaymentT0IdentityFactory identityFactory,
            PaymentT0SnapshotService snapshotService,
            ObjectProvider<PaymentEventLifecycleOrchestrationService>
                    lifecycleProvider,
            PaymentPolicyBundle policies,
            TimeProvider timeProvider
    ) {
        this.paymentLookupPort = Objects.requireNonNull(paymentLookupPort);
        this.fundsControlService = Objects.requireNonNull(fundsControlService);
        this.treasuryResolutionService =
                Objects.requireNonNull(treasuryResolutionService);
        this.treasuryPortProvider =
                Objects.requireNonNull(treasuryPortProvider);
        this.identityFactory = Objects.requireNonNull(identityFactory);
        this.snapshotService = Objects.requireNonNull(snapshotService);
        this.lifecycleProvider = Objects.requireNonNull(lifecycleProvider);
        this.policies = Objects.requireNonNull(policies);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    public void onFundsControlRequested(PaymentId paymentId) {
        Payment payment = requirePayment(paymentId);

        if (payment.status() == PaymentStatus.FUNDS_CONTROL_PENDING) {
            fundsControlService.completeFundsControlPreparation(
                    paymentId,
                    timeProvider.now()
            );
        }
    }

    public void onTreasuryResolutionRequested(
            PaymentId paymentId,
            CorrelationId correlationId
    ) {
        Payment payment = requirePayment(paymentId);

        if (payment.status()
                != PaymentStatus.TREASURY_ACCOUNT_RESOLUTION_PENDING) {
            return;
        }

        PaymentTreasuryAccountResolutionPort treasuryPort =
                treasuryPortProvider.getIfAvailable();

        if (treasuryPort == null) {
            throw new IllegalStateException(
                    "Treasury account resolution bridge is unavailable"
            );
        }

        Instant now = timeProvider.now();
        TreasuryAccountReference resolved =
                treasuryPort.resolve(payment, now);

        String evidenceCanonical =
                payment.publicPaymentReference().value()
                        + "|" + payment.toState()
                                .allocationIntentFingerprint().value()
                        + "|" + resolved.treasuryConfigurationId()
                        + "|" + resolved.configurationVersion();

        EvidenceMetadata metadata =
                new EvidenceMetadata(
                        ExternalSystem.SIXPAY,
                        correlationId,
                        EvidenceObservationChannel
                                .PROTECTED_CONFIGURATION_RESOLUTION,
                        EvidenceFingerprint.of(
                                "v1:sha256:"
                                        + sha256(evidenceCanonical)
                        ),
                        now,
                        now
                );

        TreasuryAccountResolutionSnapshot evidence =
                new TreasuryAccountResolutionSnapshot(
                        resolved,
                        payment.toState().allocationIntentFingerprint(),
                        TreasuryResolutionOutcome.RESOLVED,
                        resolved.configurationVersion(),
                        null,
                        metadata
                );

        treasuryResolutionService.recordResolution(
                paymentId,
                evidence,
                resolved,
                null,
                now,
                policies
        );
    }

    public void onApprovedForPosting(
            PaymentId paymentId,
            CorrelationId correlationId
    ) {
        Payment payment = requirePayment(paymentId);

        if (payment.status() != PaymentStatus.APPROVED_FOR_POSTING
                && payment.status() != PaymentStatus.POSTING_PENDING) {
            return;
        }

        TreasuryAccountReference treasury =
                payment.toState()
                        .treasuryAccountReference()
                        .orElseThrow();

        PostingInstructionIdentity instruction =
                payment.toState()
                        .postingInstruction()
                        .orElseGet(
                                () -> identityFactory.create(
                                        payment,
                                        treasury
                                )
                        );

        Instant now = timeProvider.now();

        snapshotService.getOrCreateFinalized(
                payment,
                treasury,
                now
        );

        PaymentEventLifecycleOrchestrationService lifecycle =
                lifecycleProvider.getIfAvailable();

        if (lifecycle == null) {
            throw new IllegalStateException(
                    "Payment T0 execution bridge is unavailable"
            );
        }

        lifecycle.execute(
                paymentId,
                instruction,
                new BankingRequestContext(
                        correlationId,
                        payment.toState().financialInstitutionCode()
                ),
                now,
                policies
        );
    }

    private Payment requirePayment(PaymentId paymentId) {
        return paymentLookupPort.findById(paymentId)
                .orElseThrow(
                        () -> new PaymentNotFoundException(paymentId)
                );
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}
