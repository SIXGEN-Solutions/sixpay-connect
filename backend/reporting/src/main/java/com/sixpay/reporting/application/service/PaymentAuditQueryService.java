package com.sixpay.reporting.application.service;

import com.sixpay.reporting.application.exception.PaymentAuditNotFoundException;
import com.sixpay.reporting.application.exception.PaymentAuditQueryUnavailableException;
import com.sixpay.reporting.application.port.input.GetPaymentAuditRecordUseCase;
import com.sixpay.reporting.application.port.input.GetPaymentTimelineUseCase;
import com.sixpay.reporting.application.port.input.SearchPaymentAuditRecordsUseCase;
import com.sixpay.reporting.application.port.output.AuditCursorCodec;
import com.sixpay.reporting.application.port.output.PaymentAuditReadPort;
import com.sixpay.reporting.application.query.*;

import java.util.Objects;
import java.util.function.Supplier;

public final class PaymentAuditQueryService
        implements GetPaymentTimelineUseCase,
        SearchPaymentAuditRecordsUseCase,
        GetPaymentAuditRecordUseCase {

    private final PaymentAuditReadPort readPort;
    private final AuditCursorCodec cursorCodec;

    public PaymentAuditQueryService(
            PaymentAuditReadPort readPort,
            AuditCursorCodec cursorCodec
    ) {
        this.readPort = Objects.requireNonNull(readPort, "readPort is required");
        this.cursorCodec = Objects.requireNonNull(
                cursorCodec,
                "cursorCodec is required"
        );
    }

    @Override
    public PaymentTimelinePage getTimeline(
            PaymentTimelineQuery query
    ) {
        PaymentTimelineQuery requested =
                Objects.requireNonNull(query, "query is required");

        return execute(() -> {
            TimelineCriteria criteria =
                    cursorCodec.decodeTimeline(requested);

            if (!readPort.paymentExists(criteria.paymentId())) {
                throw new PaymentAuditNotFoundException(
                        "Payment audit evidence was not found"
                );
            }

            TimelineSlice slice = readPort.timeline(criteria);
            AuditCursor next = slice.hasMore()
                    ? cursorCodec.encodeTimeline(
                            criteria,
                            slice.nextPosition()
                    )
                    : null;

            return new PaymentTimelinePage(
                    slice.items(),
                    slice.items().size(),
                    slice.hasMore(),
                    next,
                    criteria.snapshotAt()
            );
        });
    }

    @Override
    public PaymentAuditPage search(
            PaymentAuditSearchQuery query
    ) {
        PaymentAuditSearchQuery requested =
                Objects.requireNonNull(query, "query is required");

        return execute(() -> {
            AuditSearchCriteria criteria =
                    cursorCodec.decodeSearch(requested);

            AuditSlice slice = readPort.search(criteria);
            AuditCursor next = slice.hasMore()
                    ? cursorCodec.encodeSearch(
                            criteria,
                            slice.nextPosition()
                    )
                    : null;

            return new PaymentAuditPage(
                    slice.items(),
                    slice.items().size(),
                    slice.hasMore(),
                    next,
                    criteria.snapshotAt()
            );
        });
    }

    @Override
    public PaymentAuditRecordView get(
            GetPaymentAuditRecordQuery query
    ) {
        GetPaymentAuditRecordQuery requested =
                Objects.requireNonNull(query, "query is required");

        return execute(() ->
                readPort.findById(requested.auditId())
                        .orElseThrow(() ->
                                new PaymentAuditNotFoundException(
                                        "Payment audit record was not found"
                                )
                        )
        );
    }

    private static <T> T execute(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (PaymentAuditNotFoundException
                 | IllegalArgumentException exception) {
            throw exception;
        } catch (PaymentAuditQueryUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PaymentAuditQueryUnavailableException(
                    "Payment audit query is unavailable",
                    exception
            );
        }
    }
}
