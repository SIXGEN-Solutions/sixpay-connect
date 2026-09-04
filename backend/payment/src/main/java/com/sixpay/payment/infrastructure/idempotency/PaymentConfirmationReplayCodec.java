package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public final class PaymentConfirmationReplayCodec {

    private static final String VERSION = "v1";
    private static final String NULL = "-";

    public String encode(PaymentConfirmationBankResult result) {
        return String.join(
                "|",
                VERSION,
                encodeText(result.challengeReference().value()),
                result.status().name(),
                result.businessCode().name(),
                result.deliveryChannel() == null
                        ? NULL
                        : result.deliveryChannel().name(),
                encodeInstant(result.sentAt()),
                encodeInstant(result.expiresAt()),
                encodeInstant(result.verifiedAt())
        );
    }

    public PaymentConfirmationBankResult decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment confirmation replay payload is empty"
            );
        }

        String[] values = payload.split("\\|", -1);
        if (values.length != 8 || !VERSION.equals(values[0])) {
            throw new IllegalArgumentException(
                    "Unsupported Payment confirmation replay payload"
            );
        }

        return new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference(
                        decodeText(values[1])
                ),
                ConfirmationChallengeStatus.valueOf(values[2]),
                ConfirmationBusinessCode.valueOf(values[3]),
                NULL.equals(values[4])
                        ? null
                        : ConfirmationDeliveryChannel.valueOf(
                                values[4]
                        ),
                decodeInstant(values[5]),
                decodeInstant(values[6]),
                decodeInstant(values[7])
        );
    }

    private static String encodeInstant(Instant value) {
        return value == null ? NULL : value.toString();
    }

    private static Instant decodeInstant(String value) {
        return NULL.equals(value) ? null : Instant.parse(value);
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
