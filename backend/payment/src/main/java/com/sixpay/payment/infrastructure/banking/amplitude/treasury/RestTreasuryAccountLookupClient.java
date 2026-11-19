package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import com.sixpay.common.context.CorrelationId;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/** Reuses the configured RestClient (base URL/TLS) from challenge/verify. */
public final class RestTreasuryAccountLookupClient implements TreasuryAccountLookupClient {
    private final RestClient restClient;

    public RestTreasuryAccountLookupClient(
            RestClient restClient
    ) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    @Override
    public TreasuryAccountLookupResponse lookup(
            String branchId,
            String accountNumber,
            CorrelationId correlationId
    ) {

        Objects.requireNonNull(correlationId, "correlationId");
        RestClient.RequestHeadersSpec<?> request = restClient.get()
                .uri("/api/v1/getaccountinfo/{branchid}/{accountnumber}", branchId, accountNumber)
                .header("X-Correlation-ID", correlationId.value());
        return request.retrieve().body(TreasuryAccountLookupResponse.class);

    }
}
