package com.sixpay.reporting.application.service;

import com.sixpay.reporting.application.exception.PaymentAuditNotFoundException;
import com.sixpay.reporting.application.exception.PaymentAuditQueryUnavailableException;
import com.sixpay.reporting.application.port.output.AuditCursorCodec;
import com.sixpay.reporting.application.port.output.PaymentAuditReadPort;
import com.sixpay.reporting.application.query.AuditCursor;
import com.sixpay.reporting.application.query.AuditPosition;
import com.sixpay.reporting.application.query.GetPaymentAuditRecordQuery;
import com.sixpay.reporting.application.query.PaymentAuditRecordView;
import com.sixpay.reporting.application.query.PaymentTimelinePage;
import com.sixpay.reporting.application.query.PaymentTimelineQuery;
import com.sixpay.reporting.application.query.TimelineCriteria;
import com.sixpay.reporting.application.query.TimelineSlice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentAuditQueryServiceTest {

    private static final UUID PAYMENT_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    private static final UUID AUDIT_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-09T18:00:00Z");

    @Test
    void timelineReturnsPageAndEncodesNextCursor() {
        PaymentAuditReadPort readPort = mock(PaymentAuditReadPort.class);
        AuditCursorCodec cursorCodec = mock(AuditCursorCodec.class);
        PaymentAuditQueryService service =
                new PaymentAuditQueryService(readPort, cursorCodec);

        PaymentTimelineQuery query =
                new PaymentTimelineQuery(
                        PAYMENT_ID,
                        null,
                        null,
                        null,
                        null,
                        50,
                        SNAPSHOT
                );

        TimelineCriteria criteria =
                new TimelineCriteria(
                        PAYMENT_ID,
                        null,
                        null,
                        null,
                        50,
                        SNAPSHOT,
                        null
                );

        AuditPosition nextPosition =
                new AuditPosition(
                        SNAPSHOT.minusSeconds(1),
                        AUDIT_ID
                );

        TimelineSlice slice =
                new TimelineSlice(
                        List.of(),
                        true,
                        nextPosition
                );

        AuditCursor nextCursor = new AuditCursor("next-cursor");

        when(cursorCodec.decodeTimeline(query))
                .thenReturn(criteria);
        when(readPort.paymentExists(PAYMENT_ID))
                .thenReturn(true);
        when(readPort.timeline(criteria))
                .thenReturn(slice);
        when(cursorCodec.encodeTimeline(criteria, nextPosition))
                .thenReturn(nextCursor);

        PaymentTimelinePage page = service.getTimeline(query);

        assertThat(page.items()).isEmpty();
        assertThat(page.size()).isZero();
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(nextCursor);
        assertThat(page.snapshotAt()).isEqualTo(SNAPSHOT);

        verify(readPort).paymentExists(PAYMENT_ID);
        verify(readPort).timeline(criteria);
        verify(cursorCodec).encodeTimeline(criteria, nextPosition);
    }

    @Test
    void timelineRejectsUnknownPaymentBeforeReadingTimeline() {
        PaymentAuditReadPort readPort = mock(PaymentAuditReadPort.class);
        AuditCursorCodec cursorCodec = mock(AuditCursorCodec.class);
        PaymentAuditQueryService service =
                new PaymentAuditQueryService(readPort, cursorCodec);

        PaymentTimelineQuery query =
                new PaymentTimelineQuery(
                        PAYMENT_ID,
                        null,
                        null,
                        null,
                        null,
                        50,
                        SNAPSHOT
                );

        TimelineCriteria criteria =
                new TimelineCriteria(
                        PAYMENT_ID,
                        null,
                        null,
                        null,
                        50,
                        SNAPSHOT,
                        null
                );

        when(cursorCodec.decodeTimeline(query))
                .thenReturn(criteria);
        when(readPort.paymentExists(PAYMENT_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getTimeline(query))
                .isInstanceOf(PaymentAuditNotFoundException.class);
    }

    @Test
    void getReturnsNotFoundForUnknownAuditRecord() {
        PaymentAuditReadPort readPort = mock(PaymentAuditReadPort.class);
        AuditCursorCodec cursorCodec = mock(AuditCursorCodec.class);
        PaymentAuditQueryService service =
                new PaymentAuditQueryService(readPort, cursorCodec);

        GetPaymentAuditRecordQuery query =
                new GetPaymentAuditRecordQuery(AUDIT_ID);

        when(readPort.findById(AUDIT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(query))
                .isInstanceOf(PaymentAuditNotFoundException.class);
    }

    @Test
    void wrapsUnexpectedRepositoryFailureAsUnavailable() {
        PaymentAuditReadPort readPort = mock(PaymentAuditReadPort.class);
        AuditCursorCodec cursorCodec = mock(AuditCursorCodec.class);
        PaymentAuditQueryService service =
                new PaymentAuditQueryService(readPort, cursorCodec);

        GetPaymentAuditRecordQuery query =
                new GetPaymentAuditRecordQuery(AUDIT_ID);

        when(readPort.findById(AUDIT_ID))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.get(query))
                .isInstanceOf(PaymentAuditQueryUnavailableException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
