package com.sixpay.payment.infrastructure.banking.amplitude.posting.client;

import com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration.AmplitudePostingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentProviderEntry;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentProviderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestAmplitudePaymentEventClientTest {

    @Test
    void postsPaymentEventWithRequiredSecurityAndContractHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        RestAmplitudePaymentEventClient client =
                new RestAmplitudePaymentEventClient(
                        builder.baseUrl("http://localhost").build(),
                        () -> "access-token",
                        properties()
                );

        server.expect(
                        requestTo(
                                "http://localhost/api/v1/payment-events"
                        )
                )
                .andExpect(method(HttpMethod.POST))
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
                .andExpect(
                        header(
                                "Idempotency-Key",
                                "idem-1234567890123456"
                        )
                )
                .andRespond(
                        withSuccess(
                                "{"
                                        + "\"paymentReference\":"
                                        + "\"PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV\","
                                        + "\"outcome\":\"COMPLETED\","
                                        + "\"checks\":[],"
                                        + "\"bankReference\":\"BK-1\","
                                        + "\"reasonCode\":null,"
                                        + "\"observedAt\":"
                                        + "\"2026-09-06T18:00:05Z\""
                                        + "}",
                                org.springframework.http.MediaType.APPLICATION_JSON
                        )
                );

        client.execute(
                request(),
                "11111111-1111-4111-8111-111111111111",
                "LRB",
                "idem-1234567890123456"
        );

        server.verify();
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

    private static AmplitudePaymentEventRequest request() {
        return new AmplitudePaymentEventRequest(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "v1",
                new AmplitudePaymentProviderEvent(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                        "PAY",
                        101L,
                        "XAF",
                        "VIRPAY",
                        LocalDate.of(2026, 9, 6),
                        "SIXPAY",
                        "00001-12345678901-42",
                        "00001-99999999999-17",
                        new BigDecimal("1000"),
                        "2026-09-06/PAIEMENT/"
                                + "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV/"
                                + "12345678901",
                        false
                ),
                List.of(
                        new AmplitudePaymentProviderEntry(
                                1,
                                "D",
                                "00001-12345678901-42",
                                new BigDecimal("1000"),
                                "XAF",
                                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                        ),
                        new AmplitudePaymentProviderEntry(
                                2,
                                "C",
                                "00001-99999999999-17",
                                new BigDecimal("1000"),
                                "XAF",
                                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                        )
                ),
                Instant.parse("2026-09-06T18:00:04Z")
        );
    }
}
