package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VerificationCheckTest {
    @Test void passAcceptsNoReason() {
        var c = VerificationCheck.passed(VerificationCheckType.CUSTOMER_EXISTS);
        assertEquals(VerificationCheckResult.PASS, c.result());
        assertTrue(c.failureCodeOptional().isEmpty());
    }
    @Test void passRejectsReason() {
        assertThrows(CustomerVerificationDomainException.class, () -> new VerificationCheck(VerificationCheckType.CUSTOMER_EXISTS, VerificationCheckResult.PASS, VerificationFailureCode.CUSTOMER_NOT_FOUND));
    }
    @Test void failRequiresMatchingBusinessReason() {
        assertDoesNotThrow(() -> VerificationCheck.failed(VerificationCheckType.CUSTOMER_EXISTS, VerificationFailureCode.CUSTOMER_NOT_FOUND));
        assertThrows(CustomerVerificationDomainException.class, () -> VerificationCheck.failed(VerificationCheckType.CUSTOMER_EXISTS, VerificationFailureCode.BANKING_RESPONSE_TIMEOUT));
        assertThrows(CustomerVerificationDomainException.class, () -> VerificationCheck.failed(VerificationCheckType.CUSTOMER_EXISTS, VerificationFailureCode.ACCOUNT_NOT_FOUND));
    }
    @Test void unknownAcceptsOnlyOptionalTechnicalReason() {
        assertDoesNotThrow(() -> VerificationCheck.unknown(VerificationCheckType.ACCOUNT_EXISTS));
        assertDoesNotThrow(() -> VerificationCheck.unknown(VerificationCheckType.ACCOUNT_EXISTS, VerificationFailureCode.BANKING_RESPONSE_TIMEOUT));
        assertThrows(CustomerVerificationDomainException.class, () -> VerificationCheck.unknown(VerificationCheckType.ACCOUNT_EXISTS, VerificationFailureCode.ACCOUNT_NOT_FOUND));
    }
}
