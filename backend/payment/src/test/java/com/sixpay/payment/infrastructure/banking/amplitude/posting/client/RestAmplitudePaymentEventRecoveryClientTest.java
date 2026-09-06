package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestAmplitudePaymentEventRecoveryClientTest {

    @Test
    void getsByPaymentReferenceWithRequiredHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        RestAmplitudePaymentEventRecoveryClient client =
                client(builder);

        server.expect(
                        requestTo(
                                "http://localhost/api/v1/payment-events/"
                                        + "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andExpect(
                        header(
                                "Authorization",
                                "Bearer access-token"
                        )
                )
                .andExpect(
                        header(
                                "X-Correlation-ID",
                                "11111111-1111-4111-8111-111111111111"
                        )
                )
                .andExpect(
                        header(
                                "X-Financial-Institution-Code",
                                "LRB"
                        )
                )
                .andRespond(
                        withSuccess(
                                completedBody(),
                                MediaType.APPLICATION_JSON
                        )
                );

        assertTrue(
                client.findByPaymentReference(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                        "11111111-1111-4111-8111-111111111111",
                        "LRB"
                ).isPresent()
        );

        server.verify();
    }

    @Test
    void getsByIdempotencyKeyAndMaps404ToEmpty() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        RestAmplitudePaymentEventRecoveryClient client =
                client(builder);

        server.expect(
                        requestTo(
                                "http://localhost/api/v1/payment-events/"
                                        + "idempotency/"
                                        + "idem-1234567890123456"
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        assertTrue(
                client.findByIdempotencyKey(
                        "idem-1234567890123456",
                        "11111111-1111-4111-8111-111111111111",
                        "LRB"
                ).isEmpty()
        );

        server.verify();
    }

    private static RestAmplitudePaymentEventRecoveryClient client(
            RestClient.Builder builder
    ) {
        return new RestAmplitudePaymentEventRecoveryClient(
                builder.baseUrl("http://localhost").build(),
                () -> "access-token",
                properties()
        );
    }

    private static AmplitudePostingProperties properties() {
        return new AmplitudePostingProperties(
                URI.create("http://localhost"),
                "/api/v1/payment-events",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                new AmplitudePostingProperties.Security(
                        "amplitude-payment",
                        "amplitude-mtls"
                ),
                new AmplitudePostingProperties.Contract(
                        "v1",
                        "Idempotency-Key",
                        Set.of("200"),
                        Set.of("REJECTED"),
                        Set.of("202")
                )
        );
    }

    private static String completedBody() {
        return "{"
                + "\"paymentReference\":"
                + "\"PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV\","
                + "\"outcome\":\"COMPLETED\","
                + "\"checks\":[],"
                + "\"bankReference\":\"BK-1\","
                + "\"reasonCode\":null,"
                + "\"observedAt\":"
                + "\"2026-09-06T18:00:05Z\""
                + "}";
    }
}
