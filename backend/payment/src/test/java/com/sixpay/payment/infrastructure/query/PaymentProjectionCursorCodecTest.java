package com.sixpay.payment.infrastructure.query;

import com.sixpay.payment.application.query.PaymentSearchSort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentProjectionCursorCodecTest {

    private final PaymentProjectionCursorCodec codec =
            new PaymentProjectionCursorCodec();

    @Test
    void roundTripsStableCursor() {
        UUID paymentId = UUID.randomUUID();
        Instant snapshotAt =
                Instant.parse("2026-08-02T12:00:00Z");
        Instant positionAt =
                Instant.parse("2026-08-01T12:00:00Z");

        String encoded = codec.encode(
                PaymentSearchSort.CREATED_AT_DESC,
                snapshotAt,
                positionAt,
                paymentId
        );

        var decoded = codec.decode(
                encoded,
                PaymentSearchSort.CREATED_AT_DESC
        );

        assertThat(decoded.sort()).isEqualTo(
                PaymentSearchSort.CREATED_AT_DESC
        );
        assertThat(decoded.snapshotAt()).isEqualTo(
                snapshotAt
        );
        assertThat(decoded.positionAt()).isEqualTo(
                positionAt
        );
        assertThat(decoded.paymentId()).isEqualTo(
                paymentId
        );
    }

    @Test
    void rejectsCursorUsedWithDifferentSort() {
        String encoded = codec.encode(
                PaymentSearchSort.CREATED_AT_DESC,
                Instant.parse("2026-08-02T12:00:00Z"),
                Instant.parse("2026-08-01T12:00:00Z"),
                UUID.randomUUID()
        );

        assertThatThrownBy(() ->
                codec.decode(
                        encoded,
                        PaymentSearchSort.UPDATED_AT_DESC
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "sort does not match"
                );
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() ->
                codec.decode(
                        "not-a-cursor",
                        PaymentSearchSort.CREATED_AT_DESC
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Invalid Payment search cursor"
                );
    }
}
