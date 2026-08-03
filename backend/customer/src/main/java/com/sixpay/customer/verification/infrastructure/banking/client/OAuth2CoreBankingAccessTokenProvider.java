package com.sixpay.customer.verification.infrastructure.banking.client;

import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

public final class OAuth2CoreBankingAccessTokenProvider
        implements CoreBankingAccessTokenProvider {

    private static final String PRINCIPAL_NAME =
            "sixpay-customer-verification";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String registrationId;

    public OAuth2CoreBankingAccessTokenProvider(
            OAuth2AuthorizedClientManager authorizedClientManager,
            BankingVerificationProperties properties
    ) {
        this.authorizedClientManager = authorizedClientManager;
        this.registrationId =
                properties.security().oauth2RegistrationId();
    }

    @Override
    public String accessToken() {
        OAuth2AuthorizeRequest request =
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(registrationId)
                        .principal(PRINCIPAL_NAME)
                        .build();

        OAuth2AuthorizedClient client =
                authorizedClientManager.authorize(request);

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException(
                    "Unable to obtain Core Banking access token"
            );
        }

        return client.getAccessToken().getTokenValue();
    }
}
