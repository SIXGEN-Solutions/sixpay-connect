package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.CoreBankingCodeResponse;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.CoreBankingStringDataResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Objects;

public final class RestAmplitudePaymentEventContextClient
        implements AmplitudePaymentEventContextClient {

    private static final String FINANCIAL_INSTITUTION_HEADER =
            "X-Financial-Institution-Code";

    private static final String ALLOCATE_EVENT_NUMBER_PATH =
            "/api/v1/transactions/process/"
                    + "lastnumeroeveope/{operationCode}";

    private static final String ACCOUNTING_DATE_PATH =
            "/api/v1/nomenclature/getdatecomptable";

    private static final String NIGHT_MODE_PATH =
            "/api/v1/kyc/process/modenuit";

    private final RestClient restClient;
    private final PostingAccessTokenProvider tokenProvider;

    public RestAmplitudePaymentEventContextClient(
            RestClient restClient,
            PostingAccessTokenProvider tokenProvider
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
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
                ALLOCATE_EVENT_NUMBER_PATH,
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
                ACCOUNTING_DATE_PATH,
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
                NIGHT_MODE_PATH,
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
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId
                )
                .header(
                        FINANCIAL_INSTITUTION_HEADER,
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
