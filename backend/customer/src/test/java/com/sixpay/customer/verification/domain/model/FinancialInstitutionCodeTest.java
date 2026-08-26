package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinancialInstitutionCodeTest {

    @Test
    void normalizesToUppercaseUsingPaymentCompatibleRules() {
        FinancialInstitutionCode code =
                FinancialInstitutionCode.of(" amplitude_01 ");

        assertEquals("AMPLITUDE_01", code.value());
    }

    @Test
    void rejectsInvalidCodes() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> FinancialInstitutionCode.of("A")
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> FinancialInstitutionCode.of("BANK CODE")
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> FinancialInstitutionCode.of("A".repeat(33))
        );
    }
}
