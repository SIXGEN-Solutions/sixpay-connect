package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.List;
import java.util.Objects;

public final class OAuth2PostingAccessTokenProvider
        implements PostingAccessTokenProvider {

    private final OAuth2AuthorizedClientManager manager;
    private final AmplitudePostingProperties properties;

    public OAuth2PostingAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudePostingProperties properties
    ) {
        this.manager = Objects.requireNonNull(manager);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String accessToken() {
        var principal =
                UsernamePasswordAuthenticationToken.authenticated(
                        "sixpay-payment-posting",
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

        if (client == null) {
            throw new IllegalStateException(
                    "Cannot obtain posting OAuth2 client"
            );
        }

        OAuth2AccessToken token = client.getAccessToken();
        if (token == null
                || token.getTokenValue() == null
                || token.getTokenValue().isBlank()) {
            throw new IllegalStateException(
                    "Posting OAuth2 token is unavailable"
            );
        }

        return token.getTokenValue();
    }
}
