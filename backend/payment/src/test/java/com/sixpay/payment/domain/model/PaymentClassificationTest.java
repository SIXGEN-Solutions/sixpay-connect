package com.sixpay.payment.domain.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentClassificationTest {

    @Test
    void paymentStatusContainsExactlyTheEighteenValues() {
        assertEquals(
                Set.of(
                        PaymentStatus.RECEIVED,
                        PaymentStatus.PENDING_CONFIRMATION,
                        PaymentStatus.AUTHORIZATION_CHECKING,
                        PaymentStatus.BANKING_VERIFICATION_PENDING,
                        PaymentStatus.FUNDS_CONTROL_PENDING,
                        PaymentStatus
                                .TREASURY_ACCOUNT_RESOLUTION_PENDING,
                        PaymentStatus.APPROVED_FOR_POSTING,
                        PaymentStatus.POSTING_PENDING,
                        PaymentStatus.POSTING_OUTCOME_UNKNOWN,
                        PaymentStatus.DEBIT_CONFIRMED,
                        PaymentStatus.POSTED_PENDING_TFJ,
                        PaymentStatus.REVERSAL_REQUIRED,
                        PaymentStatus.REVERSAL_PENDING,
                        PaymentStatus.REVERSAL_OUTCOME_UNKNOWN,
                        PaymentStatus.REJECTED,
                        PaymentStatus.FAILED,
                        PaymentStatus.TREASURY_INTEGRATED,
                        PaymentStatus.REVERSED
                ),
                EnumSet.allOf(PaymentStatus.class)
        );
    }

    @Test
    void onlyFourStatusesAreTerminal() {
        Set<PaymentStatus> terminal = EnumSet.noneOf(
                PaymentStatus.class
        );

        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.isTerminal()) {
                terminal.add(status);
            }
        }

        assertEquals(
                Set.of(
                        PaymentStatus.REJECTED,
                        PaymentStatus.FAILED,
                        PaymentStatus.TREASURY_INTEGRATED,
                        PaymentStatus.REVERSED
                ),
                terminal
        );
        assertFalse(
                PaymentStatus.REVERSAL_REQUIRED.isTerminal()
        );
    }

    @Test
    void failureAndSystemClassificationsAreClosed() {
        assertEquals(6, FailureCategory.values().length);
        assertEquals(9, FailureStage.values().length);
        assertEquals(5, RetryDisposition.values().length);
        assertEquals(4, ExternalSystem.values().length);

        assertTrue(
                Set.of(ExternalSystem.values()).contains(
                        ExternalSystem.TRESOR_PAY
                )
        );
        assertTrue(
                Set.of(ExternalSystem.values()).contains(
                        ExternalSystem.SIXPAY
                )
        );
    }

    @Test
    void notifiedIsNotAPaymentStatus() {
        assertFalse(
                Set.of(PaymentStatus.values())
                        .stream()
                        .anyMatch(
                                status ->
                                        status.name().equals("NOTIFIED")
                        )
        );
    }
}
