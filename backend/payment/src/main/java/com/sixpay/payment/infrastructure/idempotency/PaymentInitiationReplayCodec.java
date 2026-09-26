package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.application.view.InitiateDebitStatus;
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

    private static final String VERSION = "v2";

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
                result.status().name(),
                result.confirmationChallenge().status().name(),
                result.confirmationChallenge().businessCode().name(),
                result.confirmationChallenge().deliveryChannel() == null ? "~" : result.confirmationChallenge().deliveryChannel().name(),
                result.confirmationChallenge().sentAt() == null ? "~" : result.confirmationChallenge().sentAt().toString(),
                result.confirmationChallenge().expiresAt() == null ? "~" : result.confirmationChallenge().expiresAt().toString(),
                result.confirmationChallenge().verifiedAt() == null ? "~" : result.confirmationChallenge().verifiedAt().toString(),
                Boolean.toString(result.confirmationChallenge().replayed())
        );
    }

    public InitiateDebitResult decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment initiation replay payload is empty"
            );
        }

        String[] values = payload.split("\\|", -1);

        if (values.length != 15
                || !VERSION.equals(values[0])) {
            throw new IllegalArgumentException(
                    "Unsupported Payment initiation replay payload"
            );
        }

        InitiateDebitStatus status =
                InitiateDebitStatus.valueOf(values[7]);

        PublicPaymentReference paymentReference = PublicPaymentReference.of(values[2]);
        com.sixpay.payment.application.view.PaymentConfirmationView challenge = new com.sixpay.payment.application.view.PaymentConfirmationView(
                paymentReference,
                com.sixpay.payment.domain.model.ConfirmationChallengeStatus.valueOf(values[8]),
                com.sixpay.payment.domain.model.ConfirmationBusinessCode.valueOf(values[9]),
                "~".equals(values[10]) ? null : com.sixpay.payment.domain.model.ConfirmationDeliveryChannel.valueOf(values[10]),
                "~".equals(values[11]) ? null : Instant.parse(values[11]),
                "~".equals(values[12]) ? null : Instant.parse(values[12]),
                "~".equals(values[13]) ? null : Instant.parse(values[13]),
                Boolean.parseBoolean(values[14]));

        return new InitiateDebitResult(
                PaymentId.from(values[1]), paymentReference, decodeText(values[3]),
                Money.of(new java.math.BigDecimal(values[4]), values[5]),
                Instant.parse(values[6]), status, challenge);
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
