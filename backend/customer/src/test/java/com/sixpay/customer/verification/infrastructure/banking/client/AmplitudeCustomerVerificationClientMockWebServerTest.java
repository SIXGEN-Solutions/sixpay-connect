package com.sixpay.customer.verification.infrastructure.banking.client;

import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationRequest;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeClientException;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeInvalidResponseException;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeRateLimitException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AmplitudeCustomerVerificationClientMockWebServerTest {

    private MockWebServer server;
    private AmplitudeCustomerVerificationClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        BankingVerificationProperties properties =
                new BankingVerificationProperties(
                        URI.create("https://amplitude.test"),
                        "/v1/accounts/verify",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2),
                        3,
                        Duration.ofMillis(10),
                        Duration.ofMinutes(5),
                        new BankingVerificationProperties.Security(
                                "core-banking-customer-verification",
                                "core-banking-client"
                        ),
                        new BankingVerificationProperties.Contract(
                                "provisional-v1",
                                Set.of("00"),
                                Set.of("01", "02", "03", "04")
                        )
                );

        client = new AmplitudeCustomerVerificationClient(
                RestClient.builder()
                        .baseUrl(server.url("/").toString())
                        .build(),
                () -> "sandbox-token",
                properties,
                JsonMapper.builder().build()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsBearerCorrelationAndRequestHeaders() throws Exception {
        server.enqueue(json(200, successBody()));

        client.verify(
                request(),
                "corr-001",
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                )
        );

        var recorded = server.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath())
                .isEqualTo("/v1/accounts/verify");
        assertThat(recorded.getHeader("Authorization"))
                .isEqualTo("Bearer sandbox-token");
        assertThat(recorded.getHeader("X-Correlation-ID"))
                .isEqualTo("corr-001");
        assertThat(recorded.getHeader("X-Request-ID"))
                .isEqualTo(
                        "00000000-0000-0000-0000-000000000001"
                );
    }

    @Test
    void exposesStructuredHttpErrors() {
        for (int status : new int[]{
                401, 403, 404, 409, 500, 503
        }) {
            server.enqueue(json(
                    status,
                    """
                    {
                      "code": "AMPLITUDE_ERROR",
                      "message": "Rejected",
                      "correlationId": "corr-001"
                    }
                    """
            ));

            assertThatThrownBy(() -> verify())
                    .isInstanceOf(
                            AmplitudeClientException.class
                    )
                    .extracting("httpStatus")
                    .isEqualTo(status);
        }
    }

    @Test
    void exposesRateLimitAndRetryAfter() {
        server.enqueue(
                json(429, "{}")
                        .addHeader("Retry-After", "7")
        );

        assertThatThrownBy(this::verify)
                .isInstanceOf(
                        AmplitudeRateLimitException.class
                )
                .extracting("retryAfter")
                .isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    void rejectsEmptySuccessfulResponse() {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .addHeader(
                                "Content-Type",
                                "application/json"
                        )
        );

        assertThatThrownBy(this::verify)
                .isInstanceOf(
                        AmplitudeInvalidResponseException.class
                );
    }

    @Test
    void rejectsMalformedSuccessfulResponse() {
        server.enqueue(json(200, "{not-json"));

        assertThatThrownBy(this::verify)
                .isInstanceOf(RuntimeException.class);
    }

    private void verify() {
        client.verify(
                request(),
                "corr-001",
                UUID.randomUUID()
        );
    }

    private static AmplitudeCustomerVerificationRequest request() {
        return new AmplitudeCustomerVerificationRequest(
                "ACC-001",
                "NIU-001",
                "Customer Test",
                "LRB"
        );
    }

    private static MockResponse json(
            int status,
            String body
    ) {
        return new MockResponse()
                .setResponseCode(status)
                .addHeader(
                        "Content-Type",
                        "application/json"
                )
                .setBody(body);
    }

    private static String successBody() {
        return """
                {
                  "code": "00",
                  "accountFound": true,
                  "accountStatus": "ACTIVE",
                  "accountHolder": "Customer Test",
                  "accountReferenceMasked": "****0001",
                  "currency": "XAF",
                  "availableBalance": 100000,
                  "accountBalance": 100000,
                  "canDebit": true,
                  "description": "Verified",
                  "result": "SUCCESS",
                  "observedAt": "2026-08-06T14:00:00Z",
                  "validUntil": "2026-08-06T14:05:00Z",
                  "checks": {
                    "CUSTOMER_EXISTS": "PASS",
                    "ACCOUNT_EXISTS": "PASS",
                    "ACCOUNT_IS_ACTIVE": "PASS"
                  }
                }
                """;
    }
}
