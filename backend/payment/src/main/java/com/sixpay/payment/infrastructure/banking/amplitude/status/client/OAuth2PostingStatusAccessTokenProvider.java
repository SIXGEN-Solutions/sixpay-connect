package com.sixpay.payment.infrastructure.banking.amplitude.status.client;

import com.sixpay.payment.infrastructure.banking.amplitude.status.configuration.AmplitudePostingStatusProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.List;
import java.util.Objects;

public final class OAuth2PostingStatusAccessTokenProvider
        implements PostingStatusAccessTokenProvider {

    private final OAuth2AuthorizedClientManager manager;
    private final AmplitudePostingStatusProperties properties;

    public OAuth2PostingStatusAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudePostingStatusProperties properties
    ) {
        this.manager = Objects.requireNonNull(manager);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String accessToken() {
        var principal =
                UsernamePasswordAuthenticationToken.authenticated(
                        "sixpay-payment-status",
                        "N/A",
                        List.of()
                );

        OAuth2AuthorizedClient client = manager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(
                                properties.security()
                                        .oauth2RegistrationId()
                        )
                        .principal(principal)
                        .build()
        );

        if (client == null
                || client.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Cannot obtain posting-status OAuth2 token"
            );
        }

        OAuth2AccessToken token =
                client.getAccessToken();

        if (token.getTokenValue() == null
                || token.getTokenValue().isBlank()) {
            throw new IllegalStateException(
                    "Posting-status OAuth2 token is unavailable"
            );
        }

        return token.getTokenValue();
    }
}
