package com.sixpay.payment.infrastructure.callback;

import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackDelivery;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackTransportPort;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "sixpay.payment.callback",
        name = "enabled",
        havingValue = "true"
)
public final class PaymentStatusCallbackHttpAdapter
        implements PaymentStatusCallbackTransportPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PaymentCallbackDetachedJwsSigner signer;

    public PaymentStatusCallbackHttpAdapter(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            PaymentCallbackDetachedJwsSigner signer
    ) {
        this.restClient = Objects.requireNonNull(
                restClientBuilder,
                "RestClient builder"
        ).build();
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper"
        );
        this.signer = Objects.requireNonNull(
                signer,
                "Callback signer"
        );
    }

    @Override
    public void send(PaymentStatusCallbackDelivery delivery) {
        Objects.requireNonNull(delivery, "Callback delivery");

        try {
            byte[] body = objectMapper.writeValueAsBytes(
                    delivery.message()
            );

            restClient.post()
                    .uri(delivery.callbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            "X-Correlation-ID",
                            delivery.correlationId().value()
                    )
                    .header(
                            "X-SIXPAY-Signature",
                            signer.sign(body)
                    )
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new PaymentCallbackTransportException(
                    "Payment callback delivery failed",
                    exception
            );
        }
    }
}
