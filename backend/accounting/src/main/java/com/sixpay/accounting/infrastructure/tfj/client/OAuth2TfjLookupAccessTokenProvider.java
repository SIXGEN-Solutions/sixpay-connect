package com.sixpay.accounting.infrastructure.tfj.client;

import com.sixpay.accounting.application.exception.AccountingProviderAuthenticationException;
import com.sixpay.accounting.infrastructure.tfj.configuration.AccountingTfjLookupProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import java.util.List;
import java.util.Objects;

public final class OAuth2TfjLookupAccessTokenProvider implements TfjLookupAccessTokenProvider {
    private final OAuth2AuthorizedClientManager manager;
    private final AccountingTfjLookupProperties properties;

    public OAuth2TfjLookupAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AccountingTfjLookupProperties properties
    ) {
        this.manager = Objects.requireNonNull(manager);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String accessToken() {
        var principal = UsernamePasswordAuthenticationToken.authenticated(
                "sixpay-accounting-tfj-lookup", "N/A", List.of()
        );
        OAuth2AuthorizedClient client = manager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(properties.security().oauth2RegistrationId())
                        .principal(principal)
                        .build()
        );
        if (client == null || client.getAccessToken() == null
                || client.getAccessToken().getTokenValue() == null
                || client.getAccessToken().getTokenValue().isBlank()) {
            throw new AccountingProviderAuthenticationException(
                    "Cannot obtain TFJ lookup OAuth2 token", null
            );
        }
        return client.getAccessToken().getTokenValue();
    }
}
