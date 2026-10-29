package com.sixpay.payment.infrastructure.callback;

import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackDelivery;
import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackTransportPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "sixpay.payment.callback", name = "enabled", havingValue = "true")
public final class PaymentStatusCallbackHttpAdapter implements PaymentStatusCallbackTransportPort {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PaymentCallbackHmacSigner signer;

    public PaymentStatusCallbackHttpAdapter(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, PaymentCallbackHmacSigner signer) {
        this.restClient = Objects.requireNonNull(restClientBuilder).build();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.signer = Objects.requireNonNull(signer);
    }

    @Override
    public void send(PaymentStatusCallbackDelivery delivery) {
        Objects.requireNonNull(delivery, "Callback delivery");
        try {
            byte[] body = objectMapper.writeValueAsBytes(delivery.message());
            URI uri = URI.create(delivery.callbackUrl());
            Instant timestamp = Instant.now();
            var signature = signer.sign("POST", requestTarget(uri), body, timestamp);
            restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-ID", delivery.correlationId().value())
                    .header("X-Webhook-Event-ID", delivery.message().eventId().toString())
                    .header("X-Webhook-Delivery-ID", delivery.deliveryId().toString())
                    .header("X-Webhook-Delivery-Attempt", Integer.toString(delivery.deliveryAttempt()))
                    .header("X-SIXPAY-Signature", signature.signature())
                    .header("X-SIXPAY-Signature-Key-ID", signature.keyId())
                    .header("X-SIXPAY-Signature-Timestamp", signature.timestamp())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new PaymentCallbackTransportException("Payment callback delivery failed", exception);
        }
    }

    private static String requestTarget(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) path = "/";
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }
}
