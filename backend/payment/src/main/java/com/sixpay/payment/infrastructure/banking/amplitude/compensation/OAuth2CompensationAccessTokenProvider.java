package com.sixpay.payment.infrastructure.banking.amplitude.compensation;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.List;
import java.util.Objects;

public final class OAuth2CompensationAccessTokenProvider
        implements CompensationAccessTokenProvider {

    private final OAuth2AuthorizedClientManager manager;
    private final String registrationId;

    public OAuth2CompensationAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            String registrationId
    ) {
        this.manager = Objects.requireNonNull(manager);
        if (registrationId == null || registrationId.isBlank()) {
            throw new IllegalArgumentException(
                    "OAuth2 registration ID is required"
            );
        }
        this.registrationId = registrationId.strip();
    }

    @Override
    public String accessToken() {
        var principal =
                UsernamePasswordAuthenticationToken.authenticated(
                        "sixpay-payment-compensation",
                        "N/A",
                        List.of()
                );

        OAuth2AuthorizedClient client = manager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(registrationId)
                        .principal(principal)
                        .build()
        );

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Cannot obtain compensation OAuth2 token"
            );
        }

        OAuth2AccessToken token = client.getAccessToken();
        if (token.getTokenValue() == null
                || token.getTokenValue().isBlank()) {
            throw new IllegalStateException(
                    "Compensation OAuth2 token is unavailable"
            );
        }

        return token.getTokenValue();
    }
}
