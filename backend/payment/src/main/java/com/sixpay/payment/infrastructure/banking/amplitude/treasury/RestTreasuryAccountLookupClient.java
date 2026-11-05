package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.client.ConfirmationAccessTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/** Reuses the same configured RestClient (base URL/TLS) and token source as challenge/verify. */
public final class RestTreasuryAccountLookupClient implements TreasuryAccountLookupClient {
    private final RestClient restClient;
    private final ConfirmationAccessTokenProvider tokenProvider;

    public RestTreasuryAccountLookupClient(
            RestClient restClient,
            ConfirmationAccessTokenProvider tokenProvider
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
    }

    @Override
    public TreasuryAccountLookupResponse lookup(
            String branchId,
            String accountNumber,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(correlationId, "correlationId");
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri("/getaccountinfo/{branchid}/{accountnumber}", branchId, accountNumber)
                .header("X-Correlation-ID", correlationId.value());
        String token = tokenProvider.accessToken();
        if (token != null && !token.isBlank()) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.retrieve().body(TreasuryAccountLookupResponse.class);
    }
}
