package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Versioned replay payload codec containing no account or authentication data.
 */
@Component
public final class PaymentInitiationReplayCodec {

    private static final String VERSION = "v1";

    public String encode(InitiateDebitResult result) {
        return String.join(
                "|",
                VERSION,
                result.paymentId().toString(),
                result.paymentReference().value(),
                encodeText(result.endToEndId()),
                result.totalAmount()
                        .amount()
                        .toPlainString(),
                result.totalAmount()
                        .currency()
                        .getCurrencyCode(),
                result.initiatedAt().toString(),
                result.status().name()
        );
    }

    public InitiateDebitResult decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment initiation replay payload is empty"
            );
        }

        String[] values = payload.split("\\|", -1);

        if (values.length != 8
                || !VERSION.equals(values[0])) {
            throw new IllegalArgumentException(
                    "Unsupported Payment initiation replay payload"
            );
        }

        PaymentStatus status =
                PaymentStatus.valueOf(values[7]);

        return new InitiateDebitResult(
                PaymentId.from(values[1]),
                PublicPaymentReference.of(values[2]),
                decodeText(values[3]),
                Money.of(
                        new java.math.BigDecimal(values[4]),
                        values[5]
                ),
                Instant.parse(values[6]),
                status,
                null
        );
    }

    private static String encodeText(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        value.getBytes(StandardCharsets.UTF_8)
                );
    }

    private static String decodeText(String value) {
        return new String(
                Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }
}
