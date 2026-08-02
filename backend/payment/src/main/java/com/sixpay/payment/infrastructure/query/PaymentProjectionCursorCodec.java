package com.sixpay.payment.infrastructure.query;

import com.sixpay.payment.application.query.PaymentSearchSort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Encodes stable cursor pagination state without exposing SQL details.
 */
@Component
public final class PaymentProjectionCursorCodec {

    private static final String VERSION = "v1";

    public String encode(
            PaymentSearchSort sort,
            Instant snapshotAt,
            Instant positionAt,
            UUID paymentId
    ) {
        Objects.requireNonNull(sort, "Payment search sort");
        Objects.requireNonNull(snapshotAt, "Payment snapshot instant");
        Objects.requireNonNull(positionAt, "Payment cursor instant");
        Objects.requireNonNull(paymentId, "Payment cursor ID");

        String plain = String.join(
                "|",
                VERSION,
                sort.name(),
                Long.toString(snapshotAt.toEpochMilli()),
                Long.toString(positionAt.toEpochMilli()),
                paymentId.toString()
        );

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        plain.getBytes(StandardCharsets.UTF_8)
                );
    }

    public Cursor decode(
            String encoded,
            PaymentSearchSort expectedSort
    ) {
        Objects.requireNonNull(expectedSort, "Expected Payment sort");

        if (encoded == null
                || encoded.isBlank()
                || encoded.length() > 2048) {
            throw invalidCursor(null);
        }

        try {
            String plain = new String(
                    Base64.getUrlDecoder().decode(encoded),
                    StandardCharsets.UTF_8
            );
            String[] parts = plain.split("\\|", -1);

            if (parts.length != 5
                    || !VERSION.equals(parts[0])) {
                throw invalidCursor(null);
            }

            PaymentSearchSort actualSort =
                    PaymentSearchSort.valueOf(parts[1]);

            if (actualSort != expectedSort) {
                throw new IllegalArgumentException(
                        "Payment cursor sort does not match request sort"
                );
            }

            Instant snapshotAt = Instant.ofEpochMilli(
                    Long.parseLong(parts[2])
            );
            Instant positionAt = Instant.ofEpochMilli(
                    Long.parseLong(parts[3])
            );
            UUID paymentId = UUID.fromString(parts[4]);

            if (positionAt.isAfter(snapshotAt)) {
                throw invalidCursor(null);
            }

            return new Cursor(
                    actualSort,
                    snapshotAt,
                    positionAt,
                    paymentId
            );
        } catch (IllegalArgumentException exception) {
            if ("Payment cursor sort does not match request sort"
                    .equals(exception.getMessage())) {
                throw exception;
            }
            throw invalidCursor(exception);
        }
    }

    private static IllegalArgumentException invalidCursor(
            RuntimeException cause
    ) {
        return new IllegalArgumentException(
                "Invalid Payment search cursor",
                cause
        );
    }

    public record Cursor(
            PaymentSearchSort sort,
            Instant snapshotAt,
            Instant positionAt,
            UUID paymentId
    ) {
        public Cursor {
            Objects.requireNonNull(sort, "Payment cursor sort");
            Objects.requireNonNull(
                    snapshotAt,
                    "Payment cursor snapshot instant"
            );
            Objects.requireNonNull(
                    positionAt,
                    "Payment cursor position instant"
            );
            Objects.requireNonNull(
                    paymentId,
                    "Payment cursor ID"
            );
        }
    }
}
