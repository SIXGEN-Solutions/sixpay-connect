package com.sixpay.payment.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.event.PaymentAuthorizationCheckingStarted;
import com.sixpay.payment.domain.event.PaymentCustomerConfirmationRecorded;
import com.sixpay.payment.domain.event.PaymentCustomerConfirmationRequested;
import com.sixpay.payment.domain.event.PaymentReceived;
import com.sixpay.payment.domain.exception.PaymentDomainException;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
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

        payment.recordCustomerConfirmation(
                RECEIVED_AT.plusSeconds(30)
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
    }

    @Test
    void confirmingAnAlreadyConfirmedPaymentIsIdempotent() {
        Payment payment = newPayment();

        payment.requestCustomerConfirmation(
                RECEIVED_AT
        );

        payment.recordCustomerConfirmation(
                RECEIVED_AT.plusSeconds(30)
        );

        payment.recordCustomerConfirmation(
                RECEIVED_AT.plusSeconds(31)
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
    }

    @Test
    void confirmationCannotBypassPendingConfirmation() {
        Payment payment = newPayment();

        assertThatThrownBy(() ->
                payment.recordCustomerConfirmation(
                        RECEIVED_AT.plusSeconds(1)
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
                .isEqualTo(PaymentStatus.RECEIVED);

        assertThat(payment.businessVersion())
                .isEqualTo(1L);

        assertThat(payment.domainEvents())
                .extracting(event ->
                        event.getClass().getName()
                )
                .containsExactly(
                        PaymentReceived.class.getName()
                );
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

        ExternalSubscriptionReference subscriptionReference =
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

        EvidenceFingerprint fingerprint =
                Mockito.mock(
                        EvidenceFingerprint.class
                );

        Money amount = Money.of(
                new BigDecimal("600000"),
                "XAF"
        );

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
                        CorrelationId.of(
                                UUID.randomUUID()
                                        .toString()
                        )
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
                        fingerprint
                );

        return Payment.receive(
                paymentId,
                publicReference,
                intent,
                RECEIVED_AT
        );
    }
}