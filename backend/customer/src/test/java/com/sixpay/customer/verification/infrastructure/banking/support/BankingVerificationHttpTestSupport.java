package com.sixpay.customer.verification.infrastructure.banking.support;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.application.port.out.BankingAccountAccessReference;
import com.sixpay.customer.verification.application.port.out.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.out.BankingVerificationResponse;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.CustomerIdentity;
import com.sixpay.customer.verification.domain.model.CustomerNiu;
import com.sixpay.customer.verification.domain.model.CustomerVerificationContext;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import com.sixpay.customer.verification.domain.model.CustomerVerificationSubject;
import com.sixpay.customer.verification.domain.model.FinancialInstitutionCode;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import com.sixpay.customer.verification.infrastructure.banking.AmplitudeCustomerVerificationAdapter;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.error.BankingVerificationErrorClassifier;
import com.sixpay.customer.verification.infrastructure.banking.mapper.AmplitudeCustomerVerificationMapper;
import com.sixpay.customer.verification.infrastructure.banking.observability.BankingVerificationObservation;
import com.sixpay.customer.verification.infrastructure.banking.retry.RetrySleeper;
import com.sixpay.customer.verification.infrastructure.banking.retry.RetryingBankingCustomerVerificationAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class BankingVerificationHttpTestSupport {

    public static final String CORRELATION_ID = "corr-http-4.3.5";
    public static final String ACCOUNT_REFERENCE = "10005-00001-12345678901-12";
    public static final String NIU = "M0123456";
    public static final String LEGAL_NAME = "Société ABC SARL";

    private BankingVerificationHttpTestSupport() {
    }

    public static BankingVerificationQuery query() {
        return new BankingVerificationQuery(
                new CustomerVerificationId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                CustomerVerificationSubject.of(
                        CustomerIdentity.of(
                                CustomerNiu.of(NIU),
                                LEGAL_NAME
                        )
                ),
                FinancialInstitutionCode.of("AMPLITUDE"),
                AccountBindingFingerprint.of(
                        "v1:" + "a".repeat(64)
                ),
                BankingAccountAccessReference.of(ACCOUNT_REFERENCE),
                CustomerVerificationContext.of(
                        CorrelationId.of(CORRELATION_ID),
                        null
                ),
                Instant.parse("2026-08-03T17:00:00Z")
        );
    }

    public static BankingVerificationProperties properties(
            URI baseUrl,
            Duration readTimeout,
            int maxAttempts
    ) {
        return new BankingVerificationProperties(
                baseUrl,
                "/v1/accounts/verify",
                Duration.ofMillis(250),
                readTimeout,
                maxAttempts,
                Duration.ofMillis(1),
                Duration.ofMinutes(5),
                new BankingVerificationProperties.Security(
                        "core-banking-test",
                        "core-banking-test"
                )
        );
    }

    public static RetryingBankingCustomerVerificationAdapter
            realHttpAdapter(
                    URI baseUrl,
                    Duration readTimeout,
                    int maxAttempts,
                    MeterRegistry registry,
                    RetrySleeper sleeper
            ) {

        BankingVerificationProperties properties =
                properties(baseUrl, readTimeout, maxAttempts);

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(
                properties.connectTimeout()
        );
        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl.toString())
                .requestFactory(requestFactory)
                .build();

        AmplitudeCustomerVerificationClient client =
                new AmplitudeCustomerVerificationClient(
                        restClient,
                        () -> "test-access-token",
                        properties,
                        new ObjectMapper()
                );

        AmplitudeCustomerVerificationMapper mapper =
                new AmplitudeCustomerVerificationMapper(
                        properties.evidenceTtl()
                );

        AmplitudeCustomerVerificationAdapter rawAdapter =
                new AmplitudeCustomerVerificationAdapter(
                        client,
                        mapper,
                        new BankingVerificationErrorClassifier()
                );

        return new RetryingBankingCustomerVerificationAdapter(
                rawAdapter,
                properties,
                sleeper,
                new BankingVerificationObservation(registry)
        );
    }

    public static BankingVerificationResponse successfulResponse() {
        List<VerificationCheck> checks =
                Arrays.stream(VerificationCheckType.values())
                        .map(VerificationCheck::passed)
                        .toList();

        return BankingVerificationResponse.of(
                checks,
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "f".repeat(64)
                ),
                Instant.parse("2026-08-03T17:00:01Z"),
                Instant.parse("2026-08-03T17:05:01Z")
        );
    }

    public static String successJson() {
        return responseJson(checksJson("PASS"), "SUCCESS", true);
    }

    public static String businessFailureJson() {
        String checks = checksJson("PASS")
                .replace(
                        "\"ACCOUNT_EXISTS\":\"PASS\"",
                        "\"ACCOUNT_EXISTS\":\"FAIL\""
                );

        return responseJson(checks, "FAILED", false);
    }

    public static String partialJson() {
        String checks = checksJson("PASS")
                .replace(
                        ",\"REQUIRED_KYC_VERIFIED\":\"PASS\"",
                        ""
                );

        return responseJson(checks, "INDETERMINATE", true);
    }

    public static String problemJson(
            int status,
            String code,
            boolean retryable
    ) {
        return """
                {
                  "type":"about:blank",
                  "title":"Core Banking error",
                  "status":%d,
                  "code":"%s",
                  "detail":"Technical failure",
                  "correlationId":"%s",
                  "retryable":%s
                }
                """.formatted(
                status,
                code,
                CORRELATION_ID,
                retryable
        );
    }

    private static String responseJson(
            String checks,
            String result,
            boolean found
    ) {
        return """
                {
                  "code":"200",
                  "accountFound":%s,
                  "accountStatus":"%s",
                  "accountHolder":"%s",
                  "accountReferenceMasked":"10005-*****-*******8901-12",
                  "currency":"XAF",
                  "availableBalance":1000000,
                  "accountBalance":100000,
                  "canDebit":%s,
                  "description":"Account verification completed",
                  "result":"%s",
                  "observedAt":"2026-08-03T17:00:01Z",
                  "validUntil":"2026-08-03T17:05:01Z",
                  "checks":%s
                }
                """.formatted(
                found,
                found ? "ACTIVE" : "UNKNOWN",
                LEGAL_NAME,
                found,
                result,
                checks
        );
    }

    private static String checksJson(String result) {
        return """
                {
                  "CUSTOMER_EXISTS":"%s",
                  "FINANCIAL_INSTITUTION_MATCHES":"%s",
                  "NIU_MATCHES":"%s",
                  "IDENTITY_MATCHES":"%s",
                  "ACCOUNT_EXISTS":"%s",
                  "ACCOUNT_BELONGS_TO_CUSTOMER":"%s",
                  "ACCOUNT_IS_ACTIVE":"%s",
                  "ACCOUNT_NOT_BLOCKED":"%s",
                  "ACCOUNT_NOT_OPPOSED":"%s",
                  "REQUIRED_KYC_PRESENT":"%s",
                  "REQUIRED_KYC_VERIFIED":"%s"
                }
                """.formatted(
                result, result, result, result, result, result,
                result, result, result, result, result
        ).replaceAll("\\s+", "");
    }
}
