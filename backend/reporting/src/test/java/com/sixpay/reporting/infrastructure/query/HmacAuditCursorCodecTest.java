package com.sixpay.reporting.infrastructure.query;

import com.sixpay.reporting.application.query.*;
import com.sixpay.reporting.domain.model.AuditSort;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HmacAuditCursorCodecTest {

    private final HmacAuditCursorCodec codec =
            new HmacAuditCursorCodec(
                    "0123456789abcdef0123456789abcdef"
                            .getBytes(StandardCharsets.UTF_8)
            );

    @Test
    void searchContinuationRestoresSnapshot() {
        Instant snapshot =
                Instant.parse("2026-08-07T20:00:00Z");

        PaymentAuditSearchQuery first =
                query(null, snapshot);

        AuditSearchCriteria criteria =
                codec.decodeSearch(first);

        AuditCursor cursor = codec.encodeSearch(
                criteria,
                new AuditPosition(
                        Instant.parse(
                                "2026-08-07T19:00:00Z"
                        ),
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        )
                )
        );

        PaymentAuditSearchQuery continuation =
                query(cursor, null);

        AuditSearchCriteria restored =
                codec.decodeSearch(continuation);

        assertEquals(snapshot, restored.snapshotAt());
        assertNotNull(restored.position());
    }

    @Test
    void tamperedCursorIsRejected() {
        Instant snapshot =
                Instant.parse("2026-08-07T20:00:00Z");

        AuditSearchCriteria criteria =
                codec.decodeSearch(query(null, snapshot));

        AuditCursor cursor = codec.encodeSearch(
                criteria,
                new AuditPosition(
                        snapshot.minusSeconds(60),
                        UUID.randomUUID()
                )
        );

        String value = cursor.value();
        char replacement =
                value.charAt(0) == 'A' ? 'B' : 'A';

        AuditCursor tampered = new AuditCursor(
                replacement + value.substring(1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> codec.decodeSearch(
                        query(tampered, null)
                )
        );
    }

    private static PaymentAuditSearchQuery query(
            AuditCursor cursor,
            Instant snapshot
    ) {
        return new PaymentAuditSearchQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.parse(
                        "2026-08-01T00:00:00Z"
                ),
                Instant.parse(
                        "2026-08-07T23:59:59Z"
                ),
                AuditSort.OCCURRED_AT_DESC,
                cursor,
                50,
                snapshot
        );
    }
}
