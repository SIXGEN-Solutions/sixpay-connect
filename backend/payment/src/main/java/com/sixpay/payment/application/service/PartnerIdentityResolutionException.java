package com.sixpay.payment.application.service;

import java.util.Objects;

public final class PartnerIdentityResolutionException
        extends RuntimeException {

    private final PartnerIdentityResolutionFailure failure;

    public PartnerIdentityResolutionException(
            PartnerIdentityResolutionFailure failure,
            String message
    ) {
        super(message);
        this.failure = Objects.requireNonNull(
                failure,
                "failure is required"
        );
    }

    public PartnerIdentityResolutionFailure failure() {
        return failure;
    }
}
