package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.client;

import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.configuration.AmplitudePaymentConfirmationProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import java.util.Objects;

public final class OAuth2ConfirmationAccessTokenProvider implements ConfirmationAccessTokenProvider {
    private static final String PRINCIPAL = "sixpay-payment-confirmation";
    private final OAuth2AuthorizedClientManager manager;
    private final AmplitudePaymentConfirmationProperties properties;

    public OAuth2ConfirmationAccessTokenProvider(OAuth2AuthorizedClientManager manager, AmplitudePaymentConfirmationProperties properties) {
        this.manager = Objects.requireNonNull(manager);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String accessToken() {
        OAuth2AuthorizedClient client = manager.authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId(properties.security().oauth2RegistrationId())
                        .principal(PRINCIPAL)
                        .build()
        );
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Unable to obtain Payment Confirmation access token");
        }
        return client.getAccessToken().getTokenValue();
    }
}
