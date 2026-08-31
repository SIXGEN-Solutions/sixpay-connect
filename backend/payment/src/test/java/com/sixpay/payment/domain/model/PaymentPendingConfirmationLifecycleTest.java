package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.event.PaymentAuthorizationCheckingStarted;
import com.sixpay.payment.domain.event.PaymentBankingVerificationRequested;
import com.sixpay.payment.domain.event.PaymentCustomerConfirmationRecorded;
import com.sixpay.payment.domain.event.PaymentCustomerConfirmationRequested;
import com.sixpay.payment.domain.event.PaymentFundsControlRequested;
import com.sixpay.payment.domain.event.PaymentReceived;
import com.sixpay.payment.domain.exception.PaymentDomainException;
import org.junit.jupiter.api.Test;

import static com.sixpay.payment.domain.model.PaymentAggregateTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentPendingConfirmationLifecycleTest {

    @Test
    void receiveRemainsDurablyReceived() {
        Payment payment = newPayment();

        assertThat(payment.status()).isEqualTo(PaymentStatus.RECEIVED);
        assertThat(payment.businessVersion()).isEqualTo(1L);
        assertThat(payment.domainEvents())
                .extracting(event -> event.getClass().getName())
                .containsExactly(PaymentReceived.class.getName());
    }

    @Test
    void receivedStartsBankingVerificationBeforeConfirmation() {
        Payment payment = newPayment();

        payment.startBankingVerification(T0.plusSeconds(1));

        assertThat(payment.status())
                .isEqualTo(PaymentStatus.BANKING_VERIFICATION_PENDING);
        assertThat(payment.businessVersion()).isEqualTo(2L);
        assertThat(payment.domainEvents())
                .extracting(event -> event.getClass().getName())
                .containsExactly(
                        PaymentReceived.class.getName(),
                        PaymentBankingVerificationRequested.class.getName()
                );
    }

    @Test
    void verifiedBankingMovesToPendingConfirmation() {
        Payment payment = newPayment();
        payment.startBankingVerification(T0.plusSeconds(1));

        payment.recordBankingVerification(
                bankingVerified("4"),
                null,
                T0.plusSeconds(2),
                profiles()
        );

        assertThat(payment.status())
                .isEqualTo(PaymentStatus.PENDING_CONFIRMATION);
        assertThat(payment.toState().bankingVerificationEvidence())
                .isPresent();
        assertThat(payment.domainEvents())
                .extracting(event -> event.getClass().getName())
                .contains(
                        PaymentCustomerConfirmationRequested.class.getName()
                );
    }

    @Test
    void otpConfirmationStartsAuthorizationChecking() {
        Payment payment = authorizationCheckingPayment();

        assertThat(payment.status())
                .isEqualTo(PaymentStatus.AUTHORIZATION_CHECKING);
        assertThat(payment.domainEvents())
                .extracting(event -> event.getClass().getName())
                .contains(
                        PaymentCustomerConfirmationRecorded.class.getName(),
                        PaymentAuthorizationCheckingStarted.class.getName()
                );
    }

    @Test
    void approvedAuthorizationMovesToFundsControlPending() {
        Payment payment = authorizationCheckingPayment();

        payment.recordAuthorizationDecision(
                authorizationApproved("3"),
                null,
                T0.plusSeconds(4),
                profiles()
        );

        assertThat(payment.status())
                .isEqualTo(PaymentStatus.FUNDS_CONTROL_PENDING);
        assertThat(payment.domainEvents())
                .extracting(event -> event.getClass().getName())
                .contains(PaymentFundsControlRequested.class.getName());
    }

    @Test
    void authorizationCannotBypassBankingAndOtp() {
        Payment payment = newPayment();

        assertThatThrownBy(() ->
                payment.startAuthorizationChecking(T0.plusSeconds(1))
        )
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("RECEIVED");

        assertThat(payment.status()).isEqualTo(PaymentStatus.RECEIVED);
    }

    @Test
    void confirmationCannotBypassBankingVerification() {
        Payment payment = newPayment();

        assertThatThrownBy(() ->
                payment.recordCustomerConfirmation(T0.plusSeconds(1))
        )
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("RECEIVED");

        assertThat(payment.status()).isEqualTo(PaymentStatus.RECEIVED);
    }
}
