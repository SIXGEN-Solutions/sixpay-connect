package com.sixpay.customer.verification.application.port.input;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.application.port.output.BankingAccountAccessReference;
import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerifyCustomerCommandTest {

    @Test
    void keepsCorrelationBindingAndOpaqueReferenceUnchanged() {
        VerifyCustomerCommand command = validCommand();
        var query = command.toBankingQuery();

        assertEquals(command.verificationId(), query.verificationId());
        assertEquals(command.accountBindingFingerprint(), query.accountBindingFingerprint());
        assertEquals(command.bankingAccountAccessReference(), query.bankingAccountAccessReference());
        assertEquals(command.context().correlationId(), query.context().correlationId());
    }

    @Test
    void rejectsMissingValuesAndRedactsSensitiveData() {
        VerifyCustomerCommand valid = validCommand();
        assertThrows(NullPointerException.class, () -> new VerifyCustomerCommand(
                null, valid.subject(), valid.financialInstitutionCode(),
                valid.accountBindingFingerprint(), valid.bankingAccountAccessReference(),
                valid.context(), valid.requestedAt()
        ));

        String rendered = valid.toString();
        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Ada Lovelace"));
        assertFalse(rendered.contains("AMP-ACC-000123"));
        assertFalse(rendered.contains("v1:" + "a".repeat(64)));
    }

    private static VerifyCustomerCommand validCommand() {
        return new VerifyCustomerCommand(
                new CustomerVerificationId(UUID.fromString("7ed75090-8af7-4dfa-9b62-8e4dca73501a")),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(CustomerNiu.of("M0123456"), "Ada Lovelace")
                ),
                FinancialInstitutionCode.of("AMPLITUDE"),
                AccountBindingFingerprint.of("v1:" + "a".repeat(64)),
                BankingAccountAccessReference.of("AMP-ACC-000123"),
                CustomerVerificationContext.of(CorrelationId.of("corr-4.4.1"), null),
                Instant.parse("2026-08-03T18:00:00Z")
        );
    }
}
