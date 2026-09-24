package com.sixpay.accounting.infrastructure.partnerstatus.client;

import com.sixpay.accounting.infrastructure.partnerstatus.configuration.PartnerStatusProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.List;
import java.util.Objects;

public final class OAuth2PartnerStatusAccessTokenProvider
        implements PartnerStatusAccessTokenProvider {

    private final OAuth2AuthorizedClientManager manager;
    private final PartnerStatusProperties properties;

    public OAuth2PartnerStatusAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            PartnerStatusProperties properties
    ) {
        this.manager = Objects.requireNonNull(manager);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String accessToken() {
        var principal =
                UsernamePasswordAuthenticationToken.authenticated(
                        "sixpay-accounting-partner-status",
                        "N/A",
                        List.of()
                );

        OAuth2AuthorizedClient client =
                manager.authorize(
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
                    "Cannot obtain Partner OAuth2 client"
            );
        }

        OAuth2AccessToken token = client.getAccessToken();
        if (token == null
                || token.getTokenValue() == null
                || token.getTokenValue().isBlank()) {
            throw new IllegalStateException(
                    "Partner OAuth2 token is unavailable"
            );
        }

        return token.getTokenValue();
    }
}
