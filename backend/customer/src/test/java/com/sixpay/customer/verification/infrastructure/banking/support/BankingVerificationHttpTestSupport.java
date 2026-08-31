package com.sixpay.customer.verification.infrastructure.banking.support;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.verification.application.port.output.*;
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
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeResponseValidator;
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
import java.util.Set;
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
                "/api/v1/customer-verifications",
                Duration.ofMillis(10),
                readTimeout,
                maxAttempts,
                Duration.ofMillis(1),
                Duration.ofMinutes(5),
                new BankingVerificationProperties.Security(
                        "core-banking-test",
                        "core-banking-test"
                ),
                new BankingVerificationProperties.Contract(
                        "test-v1",
                        Set.of(
                                "00",
                                "200"
                        ),
                        Set.of(
                                "01",
                                "02",
                                "03",
                                "04"
                        )
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
                properties(
                        baseUrl,
                        readTimeout,
                        maxAttempts
                );

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

        AmplitudeResponseValidator responseValidator =
                new AmplitudeResponseValidator();

        AmplitudeCustomerVerificationAdapter rawAdapter =
                new AmplitudeCustomerVerificationAdapter(
                        client,
                        mapper,
                        responseValidator,
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

        Instant observed = Instant.parse("2026-08-03T17:00:01Z");
        return BankingVerificationResponse.of(
                checks,
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "f".repeat(64)
                ),
                observed,
                Instant.parse("2026-08-03T17:05:01Z"),
                "CUST-0001",
                ACCOUNT_REFERENCE,
                new VerifiedBankingIdentity(
                        "CUST-0001", "000001", "AMPLITUDE", NIU, LEGAL_NAME,
                        "+237690000001", "customer@example.test", "COMPLETE", List.of(), observed, observed
                ),
                new VerifiedBankingAccount(
                        ACCOUNT_REFERENCE, "CUST-0001", "AMPLITUDE", "****8901",
                        "XAF", "CURRENT", "ACTIVE", List.of(), observed
                )
        );
    }

    public static String successJson() {
        return responseJson("VERIFIED", checksJson("PASS"), true);
    }

    public static String businessFailureJson() {
        String checks = checksJson("PASS").replace(
                "{\"type\":\"ACCOUNT_EXISTS\",\"result\":\"PASS\"}",
                "{\"type\":\"ACCOUNT_EXISTS\",\"result\":\"FAIL\"}"
        );
        return responseJson("REJECTED", checks, false);
    }

    public static String partialJson() {
        String checks = checksJson("PASS").replace(
                ",{\"type\":\"REQUIRED_KYC_VERIFIED\",\"result\":\"PASS\"}",
                ""
        );
        return responseJson("INDETERMINATE", checks, false);
    }

    public static String problemJson(int status, String code, boolean ignoredRetryable) {
        return """
                {
                  "type":"about:blank",
                  "title":"Core Banking error",
                  "status":%d,
                  "code":"%s",
                  "detail":"Technical failure",
                  "correlationId":"%s"
                }
                """.formatted(status, code, CORRELATION_ID);
    }

    private static String responseJson(String outcome, String checks, boolean verified) {
        String customerReference = verified ? "\"CUST-0001\"" : "null";
        String accountReference = verified ? "\"" + ACCOUNT_REFERENCE + "\"" : "null";
        String identity = verified ? """
                {
                  "customerReference":"CUST-0001",
                  "customerNumber":"000001",
                  "financialInstitutionCode":"AMPLITUDE",
                  "niu":"%s",
                  "legalName":"%s",
                  "phoneNumber":"+237690000001",
                  "email":"customer@example.test",
                  "kycStatus":"COMPLETE",
                  "kycFields":[
                    {"code":"niu","present":true,"verified":true},
                    {"code":"legalName","present":true,"verified":true},
                    {"code":"phoneNumber","present":true,"verified":true},
                    {"code":"email","present":true,"verified":true}
                  ],
                  "source":"AMPLITUDE",
                  "retrievedAt":"2026-08-03T17:00:01Z"
                }
                """.formatted(NIU, LEGAL_NAME) : "null";
        String account = verified ? """
                {
                  "accountReference":"%s",
                  "customerReference":"CUST-0001",
                  "financialInstitutionCode":"AMPLITUDE",
                  "maskedAccountIdentifier":"****8901",
                  "currency":"XAF",
                  "accountType":"CURRENT",
                  "status":"ACTIVE",
                  "restrictions":[],
                  "source":"AMPLITUDE",
                  "retrievedAt":"2026-08-03T17:00:01Z"
                }
                """.formatted(ACCOUNT_REFERENCE) : "null";
        return """
                {
                  "verificationId":"7ed75090-8af7-4dfa-9b62-8e4dca73501a",
                  "verifiedAt":"2026-08-03T17:00:01Z",
                  "source":"AMPLITUDE",
                  "outcome":"%s",
                  "customerReference":%s,
                  "accountReference":%s,
                  "checks":%s,
                  "identity":%s,
                  "account":%s
                }
                """.formatted(outcome, customerReference, accountReference, checks, identity, account);
    }

    private static String checksJson(String result) {
        return Arrays.stream(VerificationCheckType.values())
                .map(type -> "{\"type\":\"" + type.name() + "\",\"result\":\"" + result + "\"}")
                .reduce("[", (left, right) -> "[".equals(left) ? left + right : left + "," + right)
                + "]";
    }

}
