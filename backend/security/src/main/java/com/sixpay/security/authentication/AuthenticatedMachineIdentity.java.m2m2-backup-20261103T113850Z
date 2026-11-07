package com.sixpay.security.authentication;

import com.sixpay.common.validation.Preconditions;

/**
 * Minimal trusted machine identity established by the transport security layer.
 *
 * <p>This type is deliberately distinct from {@link AuthenticatedUser}: M2M
 * Partner callers are not SIXPAY human users.</p>
 */
public record AuthenticatedMachineIdentity(
        String subject
) {

    public AuthenticatedMachineIdentity {
        subject = Preconditions.requireNonBlank(
                subject,
                "Authenticated machine subject must not be blank"
        );
    }
}
