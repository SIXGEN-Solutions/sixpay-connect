package com.sixpay.customer.verification.infrastructure.banking.retry;

import com.sixpay.customer.verification.application.exception.BankingVerificationTimeoutException;
import com.sixpay.customer.verification.application.port.output.*;
import com.sixpay.customer.verification.domain.model.*;
import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.observability.BankingVerificationObservation;
import com.sixpay.common.context.CorrelationId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentInitiationDeadlineBankingBoundaryTest {
    @Test
    void returnsAtPaymentDeadlineEvenWhenDelegateIsStillUnavailable() {
        BankingCustomerVerificationPort hanging = query -> {
            try { Thread.sleep(5000); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("late completion");
        };
        var properties = new BankingVerificationProperties(
                URI.create("https://core-banking.example"),
                "/api/v1/customer-verifications",
                Duration.ofSeconds(1), Duration.ofSeconds(1),
                1, Duration.ofMillis(10), Duration.ofMinutes(5),
                new BankingVerificationProperties.Security("registration","bundle"),
                new BankingVerificationProperties.Contract(
                        "1.0.0", Set.of("00"), Set.of("01"))
        );
        var adapter = new RetryingBankingCustomerVerificationAdapter(
                hanging, properties, duration -> {},
                new BankingVerificationObservation(new SimpleMeterRegistry())
        );
        Instant requestedAt=Instant.now();
        BankingVerificationQuery query=new BankingVerificationQuery(
                new CustomerVerificationId(UUID.randomUUID()),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(CustomerNiu.of("NIU123"),"Customer")),
                FinancialInstitutionCode.of("BANK"),
                AccountBindingFingerprint.of("v1:" + "0".repeat(64)),
                BankingAccountAccessReference.of("account"),
                CustomerVerificationContext.of(
                        CorrelationId.of("corr-123"),UUID.randomUUID()),
                requestedAt, requestedAt.plusMillis(150)
        );
        long start=System.nanoTime();
        assertThrows(BankingVerificationTimeoutException.class,
                () -> adapter.verify(query));
        long elapsed=Duration.ofNanos(System.nanoTime()-start).toMillis();
        assertTrue(elapsed < 1500, "elapsed="+elapsed+"ms");
    }
}
