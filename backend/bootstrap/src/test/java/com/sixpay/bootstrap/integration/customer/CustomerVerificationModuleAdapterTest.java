package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerVerificationModuleAdapterTest {

    @Test
    void translatesBothModuleOwnedContracts() {
        AtomicReference<com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand>
                captured = new AtomicReference<>();

        VerifyCustomerUseCase useCase = command -> {
            captured.set(command);

            return VerifyCustomerResult.of(
                    command.verificationId(),
                    VerificationOutcome.VERIFIED,
                    Arrays.stream(VerificationCheckType.values())
                            .map(VerificationCheck::passed)
                            .toList(),
                    VerificationEvidenceFingerprint.of(
                            "v1:sha256:" + "b".repeat(64)
                    ),
                    command.accountBindingFingerprint(),
                    Instant.parse("2026-08-03T18:30:01Z"),
                    Instant.parse("2026-08-03T18:35:01Z"),
                    Instant.parse("2026-08-03T18:30:02Z"),
                    "AMPLITUDE-CUSTOMER-001",
                    "AMPLITUDE-ACCOUNT-001",
                    verifiedIdentity(),
                    verifiedAccount()
            );
        };

        CustomerVerificationModuleAdapter adapter =
                new CustomerVerificationModuleAdapter(useCase);

        CustomerVerificationRequest request = request();
        CustomerVerificationResponse response =
                adapter.verify(request);

        assertEquals(
                request.verificationId(),
                captured.get().verificationId().value()
        );
        assertEquals(
                request.integrationAccountToken(),
                captured.get()
                        .bankingAccountAccessReference()
                        .value()
        );
        assertEquals(
                request.accountBindingFingerprint(),
                captured.get()
                        .accountBindingFingerprint()
                        .value()
        );
        assertEquals(
                request.correlationId(),
                captured.get()
                        .context()
                        .correlationId()
                        .value()
        );
        assertEquals(
                CustomerVerificationResponse.Outcome.VERIFIED,
                response.outcome()
        );
        assertEquals(11, response.checks().size());
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
                Instant.parse("2026-08-03T18:00:00Z"),
                Instant.parse("2026-08-03T18:30:01Z")
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
                Instant.parse("2026-08-03T18:30:01Z")
        );
    }

    private static CustomerVerificationRequest request() {
        return new CustomerVerificationRequest(
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                "M0123456",
                "Ada Lovelace",
                "AMPLITUDE",
                "v1:" + "a".repeat(64),
                "AMP-ACC-000123",
                "corr-4.4.3",
                UUID.fromString(
                        "c74e165f-df46-463e-a520-188e6df3e5ae"
                ),
                Instant.parse("2026-08-03T18:30:00Z")
        );
    }
}
