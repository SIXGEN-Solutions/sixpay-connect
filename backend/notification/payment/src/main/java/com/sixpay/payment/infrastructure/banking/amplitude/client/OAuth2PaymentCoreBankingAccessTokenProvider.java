package com.sixpay.payment.infrastructure.banking.amplitude.client;

import com.sixpay.payment.infrastructure.banking.amplitude.configuration.AmplitudePaymentBankingProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.List;
import java.util.Objects;

public final class OAuth2PaymentCoreBankingAccessTokenProvider
        implements PaymentCoreBankingAccessTokenProvider {

    private final OAuth2AuthorizedClientManager manager;
    private final AmplitudePaymentBankingProperties properties;

    public OAuth2PaymentCoreBankingAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudePaymentBankingProperties properties
    ) {
        this.manager = Objects.requireNonNull(manager);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String accessToken() {
        String registrationId =
                properties.security().oauth2RegistrationId();

        Authentication principal =
                UsernamePasswordAuthenticationToken.authenticated(
                        "sixpay-payment",
                        "N/A",
                        List.of()
                );

        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(registrationId)
                        .principal(principal)
                        .build();

        OAuth2AuthorizedClient client =
                manager.authorize(request);

        if (client == null) {
            throw new IllegalStateException(
                    "Cannot obtain Core Banking OAuth2 client"
            );
        }

        OAuth2AccessToken token = client.getAccessToken();
        if (token == null
                || token.getTokenValue() == null
                || token.getTokenValue().isBlank()) {
            throw new IllegalStateException(
                    "Core Banking access token is unavailable"
            );
        }

        return token.getTokenValue();
    }
}
