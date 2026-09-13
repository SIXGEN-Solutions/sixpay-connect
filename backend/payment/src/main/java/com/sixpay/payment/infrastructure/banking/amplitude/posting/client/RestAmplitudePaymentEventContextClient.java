package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.CoreBankingCodeResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.CoreBankingStringDataResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Objects;

public final class RestAmplitudePaymentEventContextClient
        implements AmplitudePaymentEventContextClient {

    private final RestClient restClient;
    private final PostingAccessTokenProvider tokenProvider;
    private final AmplitudePostingProperties properties;

    public RestAmplitudePaymentEventContextClient(
            RestClient restClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public CoreBankingStringDataResponse allocateNextEventNumber(
            String operationCode,
            String correlationId,
            String financialInstitutionCode
    ) {
        if (operationCode == null || operationCode.isBlank()) {
            throw new IllegalArgumentException(
                    "operationCode is required"
            );
        }

        return get(
                properties.allocateEventNumberPath(),
                CoreBankingStringDataResponse.class,
                correlationId,
                financialInstitutionCode,
                operationCode.strip()
        );
    }

    @Override
    public CoreBankingStringDataResponse getAccountingDate(
            String correlationId,
            String financialInstitutionCode
    ) {
        return get(
                properties.accountingDatePath(),
                CoreBankingStringDataResponse.class,
                correlationId,
                financialInstitutionCode
        );
    }

    @Override
    public CoreBankingCodeResponse getNightMode(
            String correlationId,
            String financialInstitutionCode
    ) {
        return get(
                properties.nightModePath(),
                CoreBankingCodeResponse.class,
                correlationId,
                financialInstitutionCode
        );
    }

    private <T> T get(
            String path,
            Class<T> responseType,
            String correlationId,
            String financialInstitutionCode,
            Object... uriVariables
    ) {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException(
                    "correlationId is required"
            );
        }
        if (financialInstitutionCode == null
                || financialInstitutionCode.isBlank()) {
            throw new IllegalArgumentException(
                    "financialInstitutionCode is required"
            );
        }

        T response = restClient.get()
                .uri(path, uriVariables)
                .accept(MediaType.APPLICATION_JSON)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + tokenProvider.accessToken()
                )
                .header(
                        properties.contract().correlationHeader(),
                        correlationId
                )
                .header(
                        properties.contract().institutionHeader(),
                        financialInstitutionCode
                )
                .retrieve()
                .body(responseType);

        if (response == null) {
            throw new IllegalStateException(
                    "Core Banking context response is empty"
            );
        }

        return response;
    }
}
