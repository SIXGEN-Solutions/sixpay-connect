package com.sixpay.payment.infrastructure.banking.amplitude.reservation.client;

import com.sixpay.payment.infrastructure.banking.amplitude.reservation.configuration.AmplitudeFundsReservationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.List;
import java.util.Objects;

public final class OAuth2FundsReservationAccessTokenProvider
        implements FundsReservationAccessTokenProvider {

    private final OAuth2AuthorizedClientManager manager;
    private final AmplitudeFundsReservationProperties properties;

    public OAuth2FundsReservationAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudeFundsReservationProperties properties
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
                        "sixpay-payment-funds-reservation",
                        "N/A",
                        List.of()
                );

        OAuth2AuthorizedClient client = manager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(registrationId)
                        .principal(principal)
                        .build()
        );

        if (client == null) {
            throw new IllegalStateException(
                    "Cannot obtain Core Banking OAuth2 client "
                            + "for funds reservation"
            );
        }

        OAuth2AccessToken token = client.getAccessToken();

        if (token == null
                || token.getTokenValue() == null
                || token.getTokenValue().isBlank()) {
            throw new IllegalStateException(
                    "Core Banking access token is unavailable "
                            + "for funds reservation"
            );
        }

        return token.getTokenValue();
    }
}
