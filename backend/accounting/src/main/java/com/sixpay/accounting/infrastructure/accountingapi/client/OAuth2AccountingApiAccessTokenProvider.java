package com.sixpay.accounting.infrastructure.accountingapi.client;

import com.sixpay.accounting.application.exception.AccountingProviderAuthenticationException;
import com.sixpay.accounting.infrastructure.accountingapi.configuration.AccountingApiProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.List;
import java.util.Objects;

public final class OAuth2AccountingApiAccessTokenProvider
        implements AccountingApiAccessTokenProvider {

    private final OAuth2AuthorizedClientManager manager;
    private final AccountingApiProperties properties;

    public OAuth2AccountingApiAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AccountingApiProperties properties
    ) {
        this.manager = Objects.requireNonNull(
                manager,
                "manager"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties"
        );
    }

    @Override
    public String accessToken() {
        try {
            var principal =
                    UsernamePasswordAuthenticationToken
                            .authenticated(
                                    "sixpay-accounting-api",
                                    "N/A",
                                    List.of()
                            );

            OAuth2AuthorizedClient client =
                    manager.authorize(
                            OAuth2AuthorizeRequest
                                    .withClientRegistrationId(
                                            properties
                                                    .security()
                                                    .oauth2RegistrationId()
                                    )
                                    .principal(principal)
                                    .build()
                    );

            if (client == null) {
                throw new AccountingProviderAuthenticationException(
                        "Cannot obtain Accounting API OAuth2 client",
                        null
                );
            }

            OAuth2AccessToken token =
                    client.getAccessToken();

            if (token == null
                    || token.getTokenValue() == null
                    || token.getTokenValue().isBlank()) {
                throw new AccountingProviderAuthenticationException(
                        "Accounting API OAuth2 token is unavailable",
                        null
                );
            }

            return token.getTokenValue();
        } catch (AccountingProviderAuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AccountingProviderAuthenticationException(
                    "Cannot obtain Accounting API OAuth2 token",
                    exception
            );
        }
    }
}
