package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentInitiationReplayCodecTest {

    @Test
    void roundTripsAcceptedResultWithoutSensitiveData() {
        PaymentInitiationReplayCodec codec =
                new PaymentInitiationReplayCodec();

        InitiateDebitResult original =
                InitiateDebitResult.accepted(
                        new PaymentId(UUID.randomUUID()),
                        PublicPaymentReference.of(
                                "PAY-1234567890ABCDEFGHJKMNPQRS"
                        ),
                        "AVI-2025-00045678",
                        Money.of(
                                new BigDecimal("600000"),
                                "XAF"
                        ),
                        Instant.parse(
                                "2026-08-03T10:30:00Z"
                        )
                );

        String encoded = codec.encode(original);
        InitiateDebitResult decoded =
                codec.decode(encoded);

        assertThat(decoded).isEqualTo(original);
        assertThat(encoded)
                .doesNotContain("10005-")
                .doesNotContain("TRESOR_PAY");
    }
}
