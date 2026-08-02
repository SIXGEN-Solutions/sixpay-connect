package com.sixpay.payment.api;

import com.sixpay.payment.api.response.PaymentQueryResponses;
import com.sixpay.payment.application.view.PaymentProjectionViews;
import org.springframework.stereotype.Component;

@Component
public final class PaymentApiMapper {

    public PaymentQueryResponses.PaymentSearchPageResponse toResponse(
            PaymentProjectionViews.SearchPage page
    ) {
        return new PaymentQueryResponses.PaymentSearchPageResponse(
                page.items().stream().map(this::toSummary).toList(),
                page.size(),
                page.hasMore(),
                page.nextCursor(),
                page.snapshotAt()
        );
    }

    public PaymentQueryResponses.PaymentDetailResponse toResponse(
            PaymentProjectionViews.Detail detail
    ) {
        var summary = detail.summary();
        return new PaymentQueryResponses.PaymentDetailResponse(
                summary.paymentId(),
                summary.paymentReference(),
                summary.tresorPayRequestId(),
                summary.observedCustomerId(),
                summary.financialInstitutionCode(),
                account(summary.debtorAccount()),
                money(summary.amount()),
                summary.status(),
                summary.reasonCode(),
                summary.createdAt(),
                summary.updatedAt(),
                summary.finalizedAt(),
                detail.correlationId(),
                detail.aggregateVersion(),
                banking(detail.bankingVerification()),
                posting(detail.posting()),
                tfj(detail.tfj()),
                detail.notifications().stream()
                        .map(this::notification)
                        .toList(),
                reversal(detail.reversal())
        );
    }

    private PaymentQueryResponses.PaymentSummaryResponse toSummary(
            PaymentProjectionViews.Summary summary
    ) {
        return new PaymentQueryResponses.PaymentSummaryResponse(
                summary.paymentId(),
                summary.paymentReference(),
                summary.tresorPayRequestId(),
                summary.observedCustomerId(),
                summary.financialInstitutionCode(),
                account(summary.debtorAccount()),
                money(summary.amount()),
                summary.status(),
                summary.reasonCode(),
                summary.createdAt(),
                summary.updatedAt(),
                summary.finalizedAt()
        );
    }

    private static PaymentQueryResponses.MoneyResponse money(
            PaymentProjectionViews.MoneyView value
    ) {
        return new PaymentQueryResponses.MoneyResponse(
                value.amount(),
                value.currency()
        );
    }

    private static PaymentQueryResponses.MaskedAccountResponse account(
            PaymentProjectionViews.MaskedAccountView value
    ) {
        return value == null ? null
                : new PaymentQueryResponses.MaskedAccountResponse(
                        value.reference(),
                        value.maskedValue()
                );
    }

    private static PaymentQueryResponses.BankingVerificationResponse banking(
            PaymentProjectionViews.BankingVerification value
    ) {
        return value == null ? null
                : new PaymentQueryResponses.BankingVerificationResponse(
                        value.verificationId(),
                        value.outcome(),
                        value.reasonCodes(),
                        value.observedAt()
                );
    }

    private static PaymentQueryResponses.PostingResponse posting(
            PaymentProjectionViews.Posting value
    ) {
        return value == null ? null
                : new PaymentQueryResponses.PostingResponse(
                        value.bankPostingReference(),
                        value.outcome(),
                        value.observedAt()
                );
    }

    private static PaymentQueryResponses.TfjResponse tfj(
            PaymentProjectionViews.Tfj value
    ) {
        return value == null ? null
                : new PaymentQueryResponses.TfjResponse(
                        value.status(),
                        value.businessDate(),
                        value.confirmedAt()
                );
    }

    private PaymentQueryResponses.NotificationResponse notification(
            PaymentProjectionViews.Notification value
    ) {
        return new PaymentQueryResponses.NotificationResponse(
                value.type(),
                value.status(),
                value.eventId(),
                value.lastAttemptAt()
        );
    }

    private static PaymentQueryResponses.ReversalResponse reversal(
            PaymentProjectionViews.Reversal value
    ) {
        return value == null ? null
                : new PaymentQueryResponses.ReversalResponse(
                        value.status(),
                        value.reversalReference(),
                        value.observedAt()
                );
    }
}
