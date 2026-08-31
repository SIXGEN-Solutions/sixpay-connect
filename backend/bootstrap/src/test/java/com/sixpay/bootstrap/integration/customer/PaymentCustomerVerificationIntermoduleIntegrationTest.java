package com.sixpay.bootstrap.integration.customer;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.application.exception.BankingVerificationTimeoutException;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationFailureCode;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.application.port.output.CustomerVerificationTechnicalException;
import com.sixpay.payment.application.service.CustomerVerificationFailureMapper;
import com.sixpay.payment.domain.model.FailureCategory;
import com.sixpay.payment.domain.model.RetryDisposition;
import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;
import com.sixpay.payment.infrastructure.customer.mapper.CustomerVerificationPaymentMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCustomerVerificationIntermoduleIntegrationTest {

    private static final UUID PAYMENT_ID = UUID.fromString(
            "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
    );
    private static final UUID VERIFICATION_ID = UUID.fromString(
            "f85d7f62-c092-3889-8eca-f3f39d19a288"
    );
    private static final String CORRELATION_ID =
            "c74e165f-df46-463e-a520-188e6df3e5ae";
    private static final String INTEGRATION_ACCOUNT_TOKEN =
            "opaque-bank-account-token";
    private static final String BINDING_FINGERPRINT =
            "v1:" + "a".repeat(64);
    private static final Instant REQUESTED_AT =
            Instant.parse("2026-08-03T20:30:00Z");
    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T20:30:01Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-03T20:30:02Z");

    @Test
    void paymentRequestReachesCustomerWithOriginalContextAndAccountReferences() {
        AtomicReference<VerifyCustomerCommand> captured =
                new AtomicReference<>();

        VerifyCustomerUseCase useCase = command -> {
            captured.set(command);
            return customerResult(
                    command,
                    VerificationOutcome.VERIFIED,
                    passedChecks()
            );
        };

        CustomerVerificationModuleAdapter adapter =
                new CustomerVerificationModuleAdapter(useCase);

        CustomerVerificationResponse response =
                adapter.verify(paymentRequest());

        assertEquals(
                VERIFICATION_ID,
                captured.get().verificationId().value()
        );
        assertEquals(
                CORRELATION_ID,
                captured.get().context().correlationId().value()
        );
        assertEquals(
                INTEGRATION_ACCOUNT_TOKEN,
                captured.get()
                        .bankingAccountAccessReference()
                        .value()
        );
        assertEquals(
                BINDING_FINGERPRINT,
                captured.get()
                        .accountBindingFingerprint()
                        .value()
        );
        assertEquals(
                CustomerVerificationResponse.Outcome.VERIFIED,
                response.outcome()
        );
    }

    @Test
    void verifiedCustomerResultBecomesVerifiedPaymentEvidence() {
        CustomerVerificationResponse response =
                adapterReturning(
                        VerificationOutcome.VERIFIED,
                        passedChecks()
                ).verify(paymentRequest());

        var snapshot = new CustomerVerificationPaymentMapper()
                .toSnapshot(
                        response,
                        CorrelationId.of(CORRELATION_ID),
                        COMPLETED_AT
                );

        var failure = new CustomerVerificationFailureMapper()
                .from(response, COMPLETED_AT);

        assertEquals(
                BankingVerificationOutcome.VERIFIED,
                snapshot.outcome()
        );
        assertEquals(
                BINDING_FINGERPRINT,
                snapshot.accountBindingFingerprint()
        );
        assertNull(failure);
    }

    @Test
    void rejectedCustomerResultCreatesNonRetryableBusinessFailure() {
        List<VerificationCheck> checks =
                withFailure(
                        VerificationCheckType.ACCOUNT_EXISTS,
                        VerificationFailureCode.ACCOUNT_NOT_FOUND
                );

        CustomerVerificationResponse response =
                adapterReturning(
                        VerificationOutcome.REJECTED,
                        checks
                ).verify(paymentRequest());

        var snapshot = new CustomerVerificationPaymentMapper()
                .toSnapshot(
                        response,
                        CorrelationId.of(CORRELATION_ID),
                        COMPLETED_AT
                );
        var failure = new CustomerVerificationFailureMapper()
                .from(response, COMPLETED_AT);

        assertEquals(
                BankingVerificationOutcome.REJECTED,
                snapshot.outcome()
        );
        assertEquals(
                FailureCategory.BUSINESS_REJECTION,
                failure.failureCategory()
        );
        assertEquals(
                RetryDisposition.NOT_RETRYABLE,
                failure.retryDisposition()
        );
        assertEquals(
                "ACCOUNT_NOT_FOUND",
                failure.failureCode().value()
        );
    }

    @Test
    void indeterminateCustomerResultCreatesRecoverableFailure() {
        List<VerificationCheck> checks =
                withUnknown(
                        VerificationCheckType.NIU_MATCHES,
                        VerificationFailureCode.TECHNICAL_RESULT_UNKNOWN
                );

        CustomerVerificationResponse response =
                adapterReturning(
                        VerificationOutcome.INDETERMINATE,
                        checks
                ).verify(paymentRequest());

        var snapshot = new CustomerVerificationPaymentMapper()
                .toSnapshot(
                        response,
                        CorrelationId.of(CORRELATION_ID),
                        COMPLETED_AT
                );
        var failure = new CustomerVerificationFailureMapper()
                .from(response, COMPLETED_AT);

        assertEquals(
                BankingVerificationOutcome.INDETERMINATE,
                snapshot.outcome()
        );
        assertEquals(
                FailureCategory.TECHNICAL_FAILURE,
                failure.failureCategory()
        );
        assertEquals(
                RetryDisposition.SAFE_RETRY,
                failure.retryDisposition()
        );
    }

    @Test
    void customerTimeoutBecomesPaymentOwnedRetryableTechnicalFailure() {
        VerifyCustomerUseCase useCase = command -> {
            throw new BankingVerificationTimeoutException(
                    "Core Banking read timeout",
                    new RuntimeException("socket timeout")
            );
        };

        var failure = assertThrows(
                CustomerVerificationTechnicalException.class,
                () -> new CustomerVerificationModuleAdapter(useCase)
                        .verify(paymentRequest())
        );

        assertEquals(
                VERIFICATION_ID,
                failure.verificationId()
        );
        assertEquals(
                CustomerVerificationTechnicalException.ErrorType.TIMEOUT,
                failure.errorType()
        );
        assertTrue(failure.retryable());
    }

    @Test
    void identicalCustomerResultProducesIdenticalPaymentEvidence() {
        CustomerVerificationModuleAdapter adapter =
                adapterReturning(
                        VerificationOutcome.VERIFIED,
                        passedChecks()
                );

        CustomerVerificationResponse firstResponse =
                adapter.verify(paymentRequest());
        CustomerVerificationResponse replayResponse =
                adapter.verify(paymentRequest());

        CustomerVerificationPaymentMapper mapper =
                new CustomerVerificationPaymentMapper();

        var first = mapper.toSnapshot(
                firstResponse,
                CorrelationId.of(CORRELATION_ID),
                firstResponse.completedAt()
        );
        var replay = mapper.toSnapshot(
                replayResponse,
                CorrelationId.of(CORRELATION_ID),
                replayResponse.completedAt()
        );

        assertEquals(first.verificationId(), replay.verificationId());
        assertEquals(first.outcome(), replay.outcome());
        assertEquals(
                first.accountBindingFingerprint(),
                replay.accountBindingFingerprint()
        );
        assertEquals(first.checks(), replay.checks());
        assertEquals(first.metadata(), replay.metadata());
    }

    private static CustomerVerificationModuleAdapter adapterReturning(
            VerificationOutcome outcome,
            List<VerificationCheck> checks
    ) {
        return new CustomerVerificationModuleAdapter(
                command -> customerResult(command, outcome, checks)
        );
    }

    private static VerifyCustomerResult customerResult(
            VerifyCustomerCommand command,
            VerificationOutcome outcome,
            List<VerificationCheck> checks
    ) {
        return VerifyCustomerResult.of(
                command.verificationId(),
                outcome,
                checks,
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "b".repeat(64)
                ),
                command.accountBindingFingerprint(),
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(300),
                COMPLETED_AT,
                outcome == VerificationOutcome.VERIFIED
                        ? "AMPLITUDE-CUSTOMER-001"
                        : null,
                outcome == VerificationOutcome.VERIFIED
                        ? "AMPLITUDE-ACCOUNT-001"
                        : null,
                outcome == VerificationOutcome.VERIFIED
                        ? verifiedIdentity()
                        : null,
                outcome == VerificationOutcome.VERIFIED
                        ? verifiedAccount()
                        : null
        );
    }

    private static VerifiedBankingIdentity verifiedIdentity() {
        return new VerifiedBankingIdentity(
                "AMPLITUDE-CUSTOMER-001",
                "CUSTOMER-001",
                "AMPLITUDE",
                "M0123456",
                "Ada Lovelace",
                "+237600000001",
                "ada.lovelace@example.test",
                "COMPLETE",
                java.util.List.of(),
                OBSERVED_AT.minusSeconds(60),
                OBSERVED_AT
        );
    }

    private static VerifiedBankingAccount verifiedAccount() {
        return new VerifiedBankingAccount(
                "AMPLITUDE-ACCOUNT-001",
                "AMPLITUDE-CUSTOMER-001",
                "AMPLITUDE",
                "****************0123",
                "XAF",
                "CURRENT",
                "ACTIVE",
                java.util.List.of(),
                OBSERVED_AT
        );
    }

    private static CustomerVerificationRequest paymentRequest() {
        return new CustomerVerificationRequest(
                VERIFICATION_ID,
                "M0123456",
                "Ada Lovelace",
                "AMPLITUDE",
                BINDING_FINGERPRINT,
                INTEGRATION_ACCOUNT_TOKEN,
                CORRELATION_ID,
                PAYMENT_ID,
                REQUESTED_AT
        );
    }

    private static List<VerificationCheck> passedChecks() {
        return Arrays.stream(VerificationCheckType.values())
                .map(VerificationCheck::passed)
                .toList();
    }

    private static List<VerificationCheck> withFailure(
            VerificationCheckType type,
            VerificationFailureCode failureCode
    ) {
        ArrayList<VerificationCheck> checks =
                new ArrayList<>(passedChecks());
        checks.set(
                type.ordinal(),
                VerificationCheck.failed(type, failureCode)
        );
        return List.copyOf(checks);
    }

    private static List<VerificationCheck> withUnknown(
            VerificationCheckType type,
            VerificationFailureCode failureCode
    ) {
        ArrayList<VerificationCheck> checks =
                new ArrayList<>(passedChecks());
        checks.set(
                type.ordinal(),
                VerificationCheck.unknown(type, failureCode)
        );
        return List.copyOf(checks);
    }
}
