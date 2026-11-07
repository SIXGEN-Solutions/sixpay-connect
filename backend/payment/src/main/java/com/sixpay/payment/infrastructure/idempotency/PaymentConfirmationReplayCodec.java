package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * JSONB-compatible durable result of a bank-owned confirmation operation.
 * No OTP or secret is stored. Legacy v1|... decoding remains available for
 * pre-existing in-memory/test representations.
 */
@Component
public final class PaymentConfirmationReplayCodec {

    private static final String VERSION = "v1";
    private static final String LEGACY_NULL = "-";
    private static final ObjectMapper JSON = new ObjectMapper();

    public String encode(PaymentConfirmationBankResult result) {
        Objects.requireNonNull(result, "Payment confirmation result");
        ReplayPayload value = new ReplayPayload(
                VERSION,
                result.challengeReference().value(),
                result.status().name(),
                result.businessCode().name(),
                result.deliveryChannel() == null ? null : result.deliveryChannel().name(),
                formatInstant(result.sentAt()),
                formatInstant(result.expiresAt()),
                formatInstant(result.verifiedAt())
        );
        try {
            // response_payload is PostgreSQL JSONB; passing unquoted v1|... is invalid.
            return JSON.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to serialize payment confirmation replay", exception);
        }
    }

    public PaymentConfirmationBankResult decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment confirmation replay payload is empty");
        }
        if (payload.startsWith("v1|")) {
            return decodeLegacy(payload);
        }
        try {
            ReplayPayload value = JSON.readValue(payload, ReplayPayload.class);
            if (value == null || !VERSION.equals(value.version())) {
                throw new IllegalArgumentException(
                        "Unsupported Payment confirmation replay payload");
            }
            return new PaymentConfirmationBankResult(
                    new ConfirmationChallengeReference(value.challengeReference()),
                    ConfirmationChallengeStatus.valueOf(value.status()),
                    ConfirmationBusinessCode.valueOf(value.businessCode()),
                    value.deliveryChannel() == null
                            ? null : ConfirmationDeliveryChannel.valueOf(value.deliveryChannel()),
                    parseInstant(value.sentAt()),
                    parseInstant(value.expiresAt()),
                    parseInstant(value.verifiedAt())
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid Payment confirmation replay JSON payload", exception);
        }
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static Instant parseInstant(String instant) {
        return instant == null ? null : Instant.parse(instant);
    }

    private static PaymentConfirmationBankResult decodeLegacy(String payload) {
        String[] values = payload.split("\\|", -1);
        if (values.length != 8 || !VERSION.equals(values[0])) {
            throw new IllegalArgumentException(
                    "Unsupported Payment confirmation replay payload");
        }
        return new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference(
                        new String(Base64.getUrlDecoder().decode(values[1]),
                                StandardCharsets.UTF_8)),
                ConfirmationChallengeStatus.valueOf(values[2]),
                ConfirmationBusinessCode.valueOf(values[3]),
                LEGACY_NULL.equals(values[4])
                        ? null : ConfirmationDeliveryChannel.valueOf(values[4]),
                parseLegacyInstant(values[5]),
                parseLegacyInstant(values[6]),
                parseLegacyInstant(values[7])
        );
    }

    private static Instant parseLegacyInstant(String value) {
        return LEGACY_NULL.equals(value) ? null : Instant.parse(value);
    }

    // Explicit immutable wire schema for this internal JSONB replay payload.
    record ReplayPayload(
            String version,
            String challengeReference,
            String status,
            String businessCode,
            String deliveryChannel,
            String sentAt,
            String expiresAt,
            String verifiedAt
    ) { }
}
