package com.sixpay.customer.verification.infrastructure.banking.client;

import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationRequest;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeClientException;
import com.sixpay.customer.verification.infrastructure.banking.support.BankingVerificationHttpTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class AmplitudeCustomerVerificationClientContractTest {

    private static final UUID REQUEST_ID = UUID.fromString(
            "9dc8e15d-3e26-4cf1-9fd8-bc88aa39ac1e"
    );

    private MockRestServiceServer server;
    private AmplitudeCustomerVerificationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        BankingVerificationProperties properties =
                BankingVerificationHttpTestSupport.properties(
                        URI.create("https://core-banking.test"),
                        Duration.ofSeconds(1),
                        3
                );

        client = new AmplitudeCustomerVerificationClient(
                builder
                        .baseUrl(properties.baseUrl().toString())
                        .build(),
                () -> "test-access-token",
                properties,
                new ObjectMapper()
        );
    }

    @Test
    void completePositiveResponseAndCorrelationHeadersAreSupported() {
        server.expect(
                        once(),
                        requestTo(
                                "https://core-banking.test"
                                        + "/api/v1/customer-verifications"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        header(
                                "Authorization",
                                "Bearer test-access-token"
                        )
                )
                .andExpect(
                        header(
                                "X-Correlation-ID",
                                BankingVerificationHttpTestSupport
                                        .CORRELATION_ID
                        )
                )
                .andExpect(
                        content().contentType(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andRespond(
                        withSuccess(
                                BankingVerificationHttpTestSupport
                                        .successJson(),
                                MediaType.APPLICATION_JSON
                        )
                );

        AmplitudeCustomerVerificationResponse response =
                client.verify(
                        request(),
                        BankingVerificationHttpTestSupport
                                .CORRELATION_ID,
                        REQUEST_ID
                );

        assertEquals("VERIFIED", response.outcome());
        assertEquals(11, response.checks().size());
        server.verify();
    }

    @Test
    void http400IsCapturedAsStructuredClientError() {
        server.expect(once(), requestTo(
                        "https://core-banking.test"
                                + "/api/v1/customer-verifications"
                ))
                .andRespond(
                        withBadRequest()
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .body(
                                        BankingVerificationHttpTestSupport
                                                .problemJson(
                                                        400,
                                                        "REQUEST_INVALID",
                                                        false
                                                )
                                )
                );

        AmplitudeClientException failure = assertThrows(
                AmplitudeClientException.class,
                () -> client.verify(
                        request(),
                        BankingVerificationHttpTestSupport
                                .CORRELATION_ID,
                        REQUEST_ID
                )
        );

        assertEquals(400, failure.httpStatus());
        assertEquals("REQUEST_INVALID", failure.error().code());
        server.verify();
    }

    @Test
    void http401IsCapturedAsStructuredClientError() {
        server.expect(once(), requestTo(
                        "https://core-banking.test"
                                + "/api/v1/customer-verifications"
                ))
                .andRespond(
                        withUnauthorizedRequest()
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .body(
                                        BankingVerificationHttpTestSupport
                                                .problemJson(
                                                        401,
                                                        "AUTHENTICATION_REQUIRED",
                                                        false
                                                )
                                )
                );

        AmplitudeClientException failure = assertThrows(
                AmplitudeClientException.class,
                () -> client.verify(
                        request(),
                        BankingVerificationHttpTestSupport
                                .CORRELATION_ID,
                        REQUEST_ID
                )
        );

        assertEquals(401, failure.httpStatus());
        assertEquals("AUTHENTICATION_REQUIRED", failure.error().code());
        server.verify();
    }

    private static AmplitudeCustomerVerificationRequest request() {
        return new AmplitudeCustomerVerificationRequest(
                BankingVerificationHttpTestSupport.ACCOUNT_REFERENCE,
                BankingVerificationHttpTestSupport.NIU,
                BankingVerificationHttpTestSupport.LEGAL_NAME,
                "AMPLITUDE"
        );
    }
}
