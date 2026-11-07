package com.sixpay.security.configuration;

public final class AuthenticationProviderPolicyValidator {

    public AuthenticationProviderPolicyValidator(
            AuthenticationCapabilitiesProperties capabilities,
            AuthenticationProviderPolicyProperties policy
    ) {
        if (policy.requireProvider() && !capabilities.anyProviderEnabled()) {
            throw new IllegalStateException(
                    "At least one authentication provider must be enabled "
                            + "when authentication provider policy requires it"
            );
        }
    }
}
