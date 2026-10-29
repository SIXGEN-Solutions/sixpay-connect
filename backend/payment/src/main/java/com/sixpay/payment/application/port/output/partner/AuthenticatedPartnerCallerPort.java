package com.sixpay.payment.application.port.output.partner;

import java.util.Optional;

/**
 * Payment-owned boundary exposing only the trusted authenticated M2M caller
 * subject. Payment must not depend directly on Security implementation types.
 */
public interface AuthenticatedPartnerCallerPort {

    Optional<String> currentAuthenticatedSubject();

    default String requireAuthenticatedSubject() {
        return currentAuthenticatedSubject()
                .filter(value -> !value.isBlank())
                .map(String::strip)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No authenticated Partner machine subject is available"
                        )
                );
    }
}
