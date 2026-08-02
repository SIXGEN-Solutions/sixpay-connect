package com.sixpay.payment.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.event.PaymentAuthorizationCheckingStarted;
import com.sixpay.payment.domain.event.PaymentCustomerConfirmationRecorded;
import com.sixpay.payment.domain.event.PaymentCustomerConfirmationRequested;
import com.sixpay.payment.domain.event.PaymentReceived;
import com.sixpay.payment.domain.exception.PaymentDomainException;
import com.sixpay.payment.domain.model.evidence.CustomerConfirmationEvidence;
import com.sixpay.payment.domain.model.evidence.CustomerConfirmationReference;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class PaymentPendingConfirmationLifecycleTest {

    private static final Instant RECEIVED_AT =
            Instant.parse("2026-08-02T18:00:00Z");

    private static final CorrelationId CORRELATION_ID =
            CorrelationId.of(
                    "11111111-1111-1111-1111-111111111111"
            );

    private static final EvidenceFingerprint
            CONFIRMATION_FINGERPRINT =
            EvidenceFingerprint.of(
                    "v1:sha256:" + "a".repeat(64)
            );

    @Test
    void movesFromReceivedToPendingConfirmation() {
        Payment payment = newPayment();

        payment.requestCustomerConfirmation(
                RECEIVED_AT
        );

        assertThat(payment.status())
                .isEqualTo(
                        PaymentStatus.PENDING_CONFIRMATION
                );

        assertThat(payment.businessVersion())
                .isEqualTo(2L);

        assertThat(payment.domainEvents())
                .extracting(event ->
                        event.getClass().getName()
                )
                .containsExactly(
                        PaymentReceived.class.getName(),
                        PaymentCustomerConfirmationRequested.class
                                .getName()
                );

        assertThat(
                payment.toState()
                        .initiationContext()
        ).isPresent();

        assertThat(
                payment.toState()
                        .customerConfirmationEvidence()
        ).isEmpty();
    }

    @Test
    void requestingConfirmationIsIdempotent() {
        Payment payment = newPayment();

        payment.requestCustomerConfirmation(
                RECEIVED_AT
        );

        payment.requestCustomerConfirmation(
                RECEIVED_AT.plusSeconds(1)
        );

        assertThat(payment.status())
                .isEqualTo(
                        PaymentStatus.PENDING_CONFIRMATION
                );

        assertThat(payment.businessVersion())
                .isEqualTo(2L);

        assertThat(payment.domainEvents())
                .extracting(event ->
                        event.getClass().getName()
                )
                .containsExactly(
                        PaymentReceived.class.getName(),
                        PaymentCustomerConfirmationRequested.class
                                .getName()
                );
    }

    @Test
    void confirmationStartsAuthorizationChecking() {
        Payment payment = newPayment();

        payment.requestCustomerConfirmation(
                RECEIVED_AT
        );

        CustomerConfirmationEvidence evidence =
                confirmationEvidence(
                        RECEIVED_AT.plusSeconds(30)
                );

        payment.recordCustomerConfirmation(
                evidence
        );

        assertThat(payment.status())
                .isEqualTo(
                        PaymentStatus.AUTHORIZATION_CHECKING
                );

        assertThat(payment.businessVersion())
                .isEqualTo(3L);

        assertThat(payment.domainEvents())
                .extracting(event ->
                        event.getClass().getName()
                )
                .containsExactly(
                        PaymentReceived.class.getName(),
                        PaymentCustomerConfirmationRequested.class
                                .getName(),
                        PaymentCustomerConfirmationRecorded.class
                                .getName(),
                        PaymentAuthorizationCheckingStarted.class
                                .getName()
                );

        assertThat(
                payment.toState()
                        .customerConfirmationEvidence()
        ).contains(evidence);
    }

    @Test
    void confirmingAnAlreadyConfirmedPaymentIsIdempotent() {
        Payment payment = newPayment();

        payment.requestCustomerConfirmation(
                RECEIVED_AT
        );

        CustomerConfirmationEvidence evidence =
                confirmationEvidence(
                        RECEIVED_AT.plusSeconds(30)
                );

        payment.recordCustomerConfirmation(
                evidence
        );

        payment.recordCustomerConfirmation(
                evidence
        );

        assertThat(payment.status())
                .isEqualTo(
                        PaymentStatus.AUTHORIZATION_CHECKING
                );

        assertThat(payment.businessVersion())
                .isEqualTo(3L);

        assertThat(payment.domainEvents())
                .extracting(event ->
                        event.getClass().getName()
                )
                .containsExactly(
                        PaymentReceived.class.getName(),
                        PaymentCustomerConfirmationRequested.class
                                .getName(),
                        PaymentCustomerConfirmationRecorded.class
                                .getName(),
                        PaymentAuthorizationCheckingStarted.class
                                .getName()
                );

        assertThat(
                payment.toState()
                        .customerConfirmationEvidence()
        ).contains(evidence);
    }

    @Test
    void conflictingConfirmationEvidenceIsRejected() {
        Payment payment = newPayment();

        payment.requestCustomerConfirmation(
                RECEIVED_AT
        );

        CustomerConfirmationEvidence originalEvidence =
                confirmationEvidence(
                        RECEIVED_AT.plusSeconds(30)
                );

        payment.recordCustomerConfirmation(
                originalEvidence
        );

        EvidenceFingerprint conflictingFingerprint =
                EvidenceFingerprint.of(
                        "v1:sha256:" + "b".repeat(64)
                );

        CustomerConfirmationEvidence conflictingEvidence =
                new CustomerConfirmationEvidence(
                        CustomerConfirmationReference.of(
                                "AMP-CONF-000002"
                        ),
                        conflictingFingerprint,
                        RECEIVED_AT.plusSeconds(31),
                        new EvidenceMetadata(
                                ExternalSystem.AMPLITUDE,
                                CORRELATION_ID,
                                EvidenceObservationChannel
                                        .DIRECT_RESPONSE,
                                conflictingFingerprint,
                                RECEIVED_AT.plusSeconds(31),
                                RECEIVED_AT.plusSeconds(31)
                        )
                );

        assertThatThrownBy(() ->
                payment.recordCustomerConfirmation(
                        conflictingEvidence
                )
        )
                .isInstanceOf(
                        PaymentDomainException.class
                )
                .hasMessageContaining(
                        "Conflicting customer confirmation evidence"
                );

        assertThat(payment.businessVersion())
                .isEqualTo(3L);

        assertThat(
                payment.toState()
                        .customerConfirmationEvidence()
        ).contains(originalEvidence);
    }

    @Test
    void confirmationCannotBypassPendingConfirmation() {
        Payment payment = newPayment();

        CustomerConfirmationEvidence evidence =
                confirmationEvidence(
                        RECEIVED_AT.plusSeconds(1)
                );

        assertThatThrownBy(() ->
                payment.recordCustomerConfirmation(
                        evidence
                )
        )
                .isInstanceOf(
                        PaymentDomainException.class
                )
                .hasMessageContaining(
                        "recordCustomerConfirmation"
                )
                .hasMessageContaining(
                        "RECEIVED"
                );

        assertThat(payment.status())
                .isEqualTo(
                        PaymentStatus.RECEIVED
                );

        assertThat(payment.businessVersion())
                .isEqualTo(1L);

        assertThat(payment.domainEvents())
                .extracting(event ->
                        event.getClass().getName()
                )
                .containsExactly(
                        PaymentReceived.class.getName()
                );

        assertThat(
                payment.toState()
                        .customerConfirmationEvidence()
        ).isEmpty();
    }

    private Payment newPayment() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.randomUUID()
                );

        PublicPaymentReference publicReference =
                Mockito.mock(
                        PublicPaymentReference.class
                );

        NewPaymentIntent intent =
                Mockito.mock(
                        NewPaymentIntent.class
                );

        ExternalPaymentReference externalReference =
                Mockito.mock(
                        ExternalPaymentReference.class
                );

        ExternalSubscriptionReference
                subscriptionReference =
                Mockito.mock(
                        ExternalSubscriptionReference.class
                );

        PaymentRequestIdentity requestIdentity =
                Mockito.mock(
                        PaymentRequestIdentity.class
                );

        FinancialInstitutionCode institution =
                Mockito.mock(
                        FinancialInstitutionCode.class
                );

        DebtorAccountReference debtor =
                Mockito.mock(
                        DebtorAccountReference.class
                );

        TreasuryAllocationIntent allocations =
                Mockito.mock(
                        TreasuryAllocationIntent.class
                );

        EvidenceFingerprint allocationFingerprint =
                EvidenceFingerprint.of(
                        "v1:sha256:" + "c".repeat(64)
                );

        Money amount = Money.of(
                new BigDecimal("600000"),
                "XAF"
        );

        PaymentInitiationContext initiationContext =
                initiationContext();

        when(intent.source())
                .thenReturn(
                        PaymentSource.TRESOR_PAY
                );

        when(intent.externalPaymentReference())
                .thenReturn(
                        externalReference
                );

        when(intent.externalSubscriptionReference())
                .thenReturn(
                        subscriptionReference
                );

        when(intent.requestIdentity())
                .thenReturn(
                        requestIdentity
                );

        when(requestIdentity.correlationId())
                .thenReturn(
                        CORRELATION_ID
                );

        when(intent.financialInstitutionCode())
                .thenReturn(
                        institution
                );

        when(intent.debtorAccountReference())
                .thenReturn(
                        debtor
                );

        when(debtor.financialInstitutionCode())
                .thenReturn(
                        institution
                );

        when(debtor.maskedDisplay())
                .thenReturn(
                        "MASKED-1234"
                );

        when(intent.requestedAmount())
                .thenReturn(
                        amount
                );

        when(intent.treasuryAllocationIntent())
                .thenReturn(
                        allocations
                );

        when(allocations.totalAmount())
                .thenReturn(
                        amount
                );

        when(intent.allocationIntentFingerprint())
                .thenReturn(
                        allocationFingerprint
                );

        when(intent.initiationContext())
                .thenReturn(
                        initiationContext
                );

        return Payment.receive(
                paymentId,
                publicReference,
                intent,
                initiationContext,
                RECEIVED_AT
        );
    }

    private PaymentInitiationContext initiationContext() {
        return new PaymentInitiationContext(
                "TRESOR_PAY",
                "TP_APP_001",
                "Société ABC SARL",
                ClaimType.AVI,
                "100200300",
                Instant.parse(
                        "2026-08-03T10:30:00Z"
                ),
                CallbackEndpoint.of(
                        "https://tresorpay.cm/"
                                + "v1/callbacks/"
                                + "payment-status"
                )
        );
    }

    private CustomerConfirmationEvidence
    confirmationEvidence(
            Instant confirmedAt
    ) {

        EvidenceMetadata metadata =
                new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        CORRELATION_ID,
                        EvidenceObservationChannel
                                .DIRECT_RESPONSE,
                        CONFIRMATION_FINGERPRINT,
                        confirmedAt,
                        confirmedAt
                );

        return new CustomerConfirmationEvidence(
                CustomerConfirmationReference.of(
                        "AMP-CONF-000001"
                ),
                CONFIRMATION_FINGERPRINT,
                confirmedAt,
                metadata
        );
    }
}