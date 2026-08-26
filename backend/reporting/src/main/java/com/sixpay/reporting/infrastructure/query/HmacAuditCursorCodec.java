package com.sixpay.reporting.infrastructure.query;

import com.sixpay.reporting.application.port.output.AuditCursorCodec;
import com.sixpay.reporting.application.query.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public final class HmacAuditCursorCodec implements AuditCursorCodec {

    private static final String HMAC = "HmacSHA256";
    private static final String TIMELINE = "T";
    private static final String SEARCH = "S";
    private static final String VERSION = "1";

    private final byte[] key;

    public HmacAuditCursorCodec(byte[] key) {
        Objects.requireNonNull(key, "key is required");
        if (key.length < 32) {
            throw new IllegalArgumentException(
                    "audit cursor key must contain at least 32 bytes"
            );
        }
        this.key = key.clone();
    }

    @Override
    public TimelineCriteria decodeTimeline(
            PaymentTimelineQuery query
    ) {
        if (query.cursor() == null) {
            return new TimelineCriteria(
                    query.paymentId(),
                    query.category(),
                    query.occurredFrom(),
                    query.occurredTo(),
                    query.size(),
                    query.snapshotAt(),
                    null
            );
        }

        Decoded decoded = decode(query.cursor(), TIMELINE);
        String fingerprint = timelineFingerprint(query);

        if (!MessageDigest.isEqual(
                decoded.fingerprint().getBytes(StandardCharsets.UTF_8),
                fingerprint.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalArgumentException(
                    "cursor query does not match the request"
            );
        }

        return new TimelineCriteria(
                query.paymentId(),
                query.category(),
                query.occurredFrom(),
                query.occurredTo(),
                query.size(),
                decoded.snapshotAt(),
                decoded.position()
        );
    }

    @Override
    public AuditSearchCriteria decodeSearch(
            PaymentAuditSearchQuery query
    ) {
        if (query.cursor() == null) {
            return criteria(query, query.snapshotAt(), null);
        }

        Decoded decoded = decode(query.cursor(), SEARCH);
        String fingerprint = searchFingerprint(query);

        if (!MessageDigest.isEqual(
                decoded.fingerprint().getBytes(StandardCharsets.UTF_8),
                fingerprint.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalArgumentException(
                    "cursor query does not match the request"
            );
        }

        return criteria(
                query,
                decoded.snapshotAt(),
                decoded.position()
        );
    }

    @Override
    public AuditCursor encodeTimeline(
            TimelineCriteria criteria,
            AuditPosition position
    ) {
        return encode(
                TIMELINE,
                criteria.snapshotAt(),
                position,
                timelineFingerprint(criteria)
        );
    }

    @Override
    public AuditCursor encodeSearch(
            AuditSearchCriteria criteria,
            AuditPosition position
    ) {
        return encode(
                SEARCH,
                criteria.snapshotAt(),
                position,
                searchFingerprint(criteria)
        );
    }

    private AuditCursor encode(
            String type,
            Instant snapshotAt,
            AuditPosition position,
            String fingerprint
    ) {
        String payload = String.join(
                "|",
                VERSION,
                type,
                Long.toString(snapshotAt.toEpochMilli()),
                Long.toString(position.occurredAt().toEpochMilli()),
                position.id().toString(),
                fingerprint
        );

        String body = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        payload.getBytes(StandardCharsets.UTF_8)
                );
        String signature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(sign(body));

        return new AuditCursor(body + "." + signature);
    }

    private Decoded decode(
            AuditCursor cursor,
            String expectedType
    ) {
        String[] token = cursor.value().split("\\.", -1);
        if (token.length != 2) {
            throw new IllegalArgumentException("cursor is malformed");
        }

        byte[] actual;
        try {
            actual = Base64.getUrlDecoder().decode(token[1]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "cursor signature is malformed",
                    exception
            );
        }

        if (!MessageDigest.isEqual(sign(token[0]), actual)) {
            throw new IllegalArgumentException(
                    "cursor signature is invalid"
            );
        }

        String payload;
        try {
            payload = new String(
                    Base64.getUrlDecoder().decode(token[0]),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "cursor payload is malformed",
                    exception
            );
        }

        String[] fields = payload.split("\\|", -1);
        if (fields.length != 6
                || !VERSION.equals(fields[0])
                || !expectedType.equals(fields[1])) {
            throw new IllegalArgumentException(
                    "cursor version or type is invalid"
            );
        }

        try {
            return new Decoded(
                    Instant.ofEpochMilli(Long.parseLong(fields[2])),
                    new AuditPosition(
                            Instant.ofEpochMilli(
                                    Long.parseLong(fields[3])
                            ),
                            UUID.fromString(fields[4])
                    ),
                    fields[5]
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "cursor payload is invalid",
                    exception
            );
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Cannot sign audit cursor",
                    exception
            );
        }
    }

    private static String timelineFingerprint(
            PaymentTimelineQuery query
    ) {
        return digest(String.join(
                "|",
                query.paymentId().toString(),
                value(query.category()),
                value(query.occurredFrom()),
                value(query.occurredTo()),
                Integer.toString(query.size())
        ));
    }

    private static String timelineFingerprint(
            TimelineCriteria criteria
    ) {
        return digest(String.join(
                "|",
                criteria.paymentId().toString(),
                value(criteria.category()),
                value(criteria.occurredFrom()),
                value(criteria.occurredTo()),
                Integer.toString(criteria.size())
        ));
    }

    private static String searchFingerprint(
            PaymentAuditSearchQuery query
    ) {
        return digest(String.join(
                "|",
                value(query.paymentId()),
                value(query.paymentReference()),
                value(query.observedCustomerId()),
                value(query.actorId()),
                value(query.actorType()),
                value(query.action()),
                value(query.result()),
                value(query.reasonCode()),
                value(query.correlationId()),
                value(query.sourceSystem()),
                query.occurredFrom().toString(),
                query.occurredTo().toString(),
                query.sort().name(),
                Integer.toString(query.size())
        ));
    }

    private static String searchFingerprint(
            AuditSearchCriteria criteria
    ) {
        return digest(String.join(
                "|",
                value(criteria.paymentId()),
                value(criteria.paymentReference()),
                value(criteria.observedCustomerId()),
                value(criteria.actorId()),
                value(criteria.actorType()),
                value(criteria.action()),
                value(criteria.result()),
                value(criteria.reasonCode()),
                value(criteria.correlationId()),
                value(criteria.sourceSystem()),
                criteria.occurredFrom().toString(),
                criteria.occurredTo().toString(),
                criteria.sort().name(),
                Integer.toString(criteria.size())
        ));
    }

    private static AuditSearchCriteria criteria(
            PaymentAuditSearchQuery query,
            Instant snapshotAt,
            AuditPosition position
    ) {
        return new AuditSearchCriteria(
                query.paymentId(),
                query.paymentReference(),
                query.observedCustomerId(),
                query.actorId(),
                query.actorType(),
                query.action(),
                query.result(),
                query.reasonCode(),
                query.correlationId(),
                query.sourceSystem(),
                query.occurredFrom(),
                query.occurredTo(),
                query.sort(),
                query.size(),
                snapshotAt,
                position
        );
    }

    private static String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private record Decoded(
            Instant snapshotAt,
            AuditPosition position,
            String fingerprint
    ) {
    }
}
