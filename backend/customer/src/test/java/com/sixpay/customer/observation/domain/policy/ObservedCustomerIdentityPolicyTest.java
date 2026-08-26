package com.sixpay.customer.observation.domain.policy;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.customer.observation.domain.model.ObservedCustomerIdentity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObservedCustomerIdentityPolicyTest {

    @Test
    void acceptsEquivalentNamesForSameNiu() {
        var current = ObservedCustomerIdentity.of(
                "M0123456",
                "Société ABC SARL",
                null,
                null
        );
        var candidate = ObservedCustomerIdentity.of(
                "M0123456",
                "SOCIETE-ABC SARL",
                null,
                null
        );

        assertTrue(
                ObservedCustomerIdentityPolicy.compatible(
                        current,
                        candidate
                )
        );
    }

    @Test
    void rejectsDifferentNiuOrConflictingName() {
        var current = ObservedCustomerIdentity.of(
                "M0123456",
                "Société ABC SARL",
                null,
                null
        );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> ObservedCustomerIdentityPolicy.requireCompatible(
                        current,
                        ObservedCustomerIdentity.of(
                                "M9999999",
                                "Société ABC SARL",
                                null,
                                null
                        )
                )
        );
    }
}
