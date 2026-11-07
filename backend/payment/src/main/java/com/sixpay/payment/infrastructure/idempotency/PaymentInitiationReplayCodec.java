package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.application.view.InitiateDebitStatus;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * JSONB-compatible versioned replay for PAYMENT_INITIATE_DEBIT.
 * Historical v2|... values remain readable. No OTP or account data is stored.
 */
@Component
public final class PaymentInitiationReplayCodec {

    private static final String VERSION = "v2";
    private static final String LEGACY_NULL = "~";
    private static final ObjectMapper JSON = new ObjectMapper();

    public String encode(InitiateDebitResult result) {
        Objects.requireNonNull(result, "Payment initiation result");
        PaymentConfirmationView confirmation = result.confirmationChallenge();
        ReplayPayload payload = new ReplayPayload(
                VERSION,
                result.paymentId().toString(),
                result.paymentReference().value(),
                result.endToEndId(),
                result.totalAmount().amount().toPlainString(),
                result.totalAmount().currency().getCurrencyCode(),
                result.initiatedAt().toString(),
                result.status().name(),
                confirmation.status().name(),
                confirmation.businessCode().name(),
                confirmation.deliveryChannel() == null
                        ? null : confirmation.deliveryChannel().name(),
                formatInstant(confirmation.sentAt()),
                formatInstant(confirmation.expiresAt()),
                formatInstant(confirmation.verifiedAt()),
                confirmation.replayed()
        );
        try {
            return JSON.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot serialize payment initiation replay JSON", exception);
        }
    }

    public InitiateDebitResult decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment initiation replay payload is empty");
        }
        if (payload.startsWith("v2|")) {
            return decodeLegacy(payload);
        }
        try {
            // Check the version before binding to a record with primitive fields:
            // an incomplete unsupported-version document must not fail on
            // a missing boolean before its version is evaluated.
            var tree = JSON.readTree(payload);
            if (tree == null || !tree.isObject()
                    || tree.get("version") == null
                    || !VERSION.equals(tree.get("version").asText())) {
                throw new IllegalArgumentException(
                        "Unsupported Payment initiation replay payload");
            }
            ReplayPayload value = JSON.readValue(payload, ReplayPayload.class);
            PublicPaymentReference paymentReference =
                    PublicPaymentReference.of(value.paymentReference());
            PaymentConfirmationView challenge = new PaymentConfirmationView(
                    paymentReference,
                    ConfirmationChallengeStatus.valueOf(value.challengeStatus()),
                    ConfirmationBusinessCode.valueOf(value.challengeBusinessCode()),
                    value.challengeDeliveryChannel() == null ? null
                            : ConfirmationDeliveryChannel.valueOf(value.challengeDeliveryChannel()),
                    parseInstant(value.challengeSentAt()),
                    parseInstant(value.challengeExpiresAt()),
                    parseInstant(value.challengeVerifiedAt()),
                    value.challengeReplayed()
            );
            return new InitiateDebitResult(
                    PaymentId.from(value.paymentId()),
                    paymentReference,
                    value.endToEndId(),
                    Money.of(new BigDecimal(value.amount()), value.currency()),
                    Instant.parse(value.initiatedAt()),
                    InitiateDebitStatus.valueOf(value.status()),
                    challenge
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid Payment initiation replay JSON", exception);
        }
    }

    /** Decoder for already materialized historical v2|... replay values. */
    private static InitiateDebitResult decodeLegacy(String payload) {
        String[] values = payload.split("\\|", -1);
        if (values.length != 15 || !VERSION.equals(values[0])) {
            throw new IllegalArgumentException(
                    "Unsupported Payment initiation replay payload");
        }
        PublicPaymentReference paymentReference =
                PublicPaymentReference.of(values[2]);
        PaymentConfirmationView challenge = new PaymentConfirmationView(
                paymentReference,
                ConfirmationChallengeStatus.valueOf(values[8]),
                ConfirmationBusinessCode.valueOf(values[9]),
                LEGACY_NULL.equals(values[10]) ? null
                        : ConfirmationDeliveryChannel.valueOf(values[10]),
                parseLegacyInstant(values[11]),
                parseLegacyInstant(values[12]),
                parseLegacyInstant(values[13]),
                Boolean.parseBoolean(values[14])
        );
        return new InitiateDebitResult(
                PaymentId.from(values[1]),
                paymentReference,
                new String(Base64.getUrlDecoder().decode(values[3]), StandardCharsets.UTF_8),
                Money.of(new BigDecimal(values[4]), values[5]),
                Instant.parse(values[6]),
                InitiateDebitStatus.valueOf(values[7]),
                challenge
        );
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static Instant parseInstant(String instant) {
        return instant == null ? null : Instant.parse(instant);
    }

    private static Instant parseLegacyInstant(String value) {
        return LEGACY_NULL.equals(value) ? null : Instant.parse(value);
    }

    /** All 15 fields of the legacy logical result, now a JSONB document. */
    record ReplayPayload(
            String version,
            String paymentId,
            String paymentReference,
            String endToEndId,
            String amount,
            String currency,
            String initiatedAt,
            String status,
            String challengeStatus,
            String challengeBusinessCode,
            String challengeDeliveryChannel,
            String challengeSentAt,
            String challengeExpiresAt,
            String challengeVerifiedAt,
            boolean challengeReplayed
    ) { }
}
