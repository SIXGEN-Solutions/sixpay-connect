package com.sixpay.customer.verification.domain.policy;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RequiredVerificationChecksPolicyTest {
    private static List<VerificationCheck> allPassed() { return Arrays.stream(VerificationCheckType.values()).map(VerificationCheck::passed).toList(); }
    @Test void returnsCanonicalImmutableSet() {
        var reversed = new ArrayList<>(allPassed()); Collections.reverse(reversed);
        var canonical = RequiredVerificationChecksPolicy.requireComplete(reversed);
        assertEquals(List.of(VerificationCheckType.values()), canonical.stream().map(VerificationCheck::type).toList());
        assertThrows(UnsupportedOperationException.class, () -> canonical.add(VerificationCheck.passed(VerificationCheckType.CUSTOMER_EXISTS)));
    }
    @Test void rejectsMissingAndDuplicateChecks() {
        var missing = new ArrayList<>(allPassed()); missing.remove(0);
        assertThrows(CustomerVerificationDomainException.class, () -> RequiredVerificationChecksPolicy.requireComplete(missing));
        var duplicate = new ArrayList<>(allPassed()); duplicate.add(VerificationCheck.passed(VerificationCheckType.CUSTOMER_EXISTS));
        assertThrows(CustomerVerificationDomainException.class, () -> RequiredVerificationChecksPolicy.requireComplete(duplicate));
    }
}
