package com.sixpay.payment.infrastructure.web;

import com.sixpay.payment.application.view.PaymentProjectionViews;
import com.sixpay.payment.infrastructure.web.dto.PaymentQueryResponses;
import org.springframework.stereotype.Component;

@Component
public final class PaymentQueryRestMapper {
    public PaymentQueryResponses.PaymentSearchPageResponse toResponse(PaymentProjectionViews.SearchPage page) {
        return new PaymentQueryResponses.PaymentSearchPageResponse(
                page.items().stream().map(this::toSummary).toList(),
                page.size(), page.hasMore(), page.nextCursor(), page.snapshotAt()
        );
    }

    public PaymentQueryResponses.PaymentDetailResponse toResponse(PaymentProjectionViews.Detail detail) {
        var s = detail.summary();
        return new PaymentQueryResponses.PaymentDetailResponse(
                s.paymentId(), s.paymentReference(), s.tresorPayRequestId(),
                s.observedCustomerId(), s.financialInstitutionCode(),
                account(s.debtorAccount()), money(s.amount()), s.status(),
                s.reasonCode(), s.createdAt(), s.updatedAt(), s.finalizedAt(),
                detail.correlationId(), detail.aggregateVersion(),
                banking(detail.bankingVerification()), posting(detail.posting()),
                tfj(detail.tfj()),
                detail.notifications().stream().map(this::notification).toList(),
                reversal(detail.reversal())
        );
    }

    private PaymentQueryResponses.PaymentSummaryResponse toSummary(PaymentProjectionViews.Summary s) {
        return new PaymentQueryResponses.PaymentSummaryResponse(
                s.paymentId(), s.paymentReference(), s.tresorPayRequestId(),
                s.observedCustomerId(), s.financialInstitutionCode(),
                account(s.debtorAccount()), money(s.amount()), s.status(),
                s.reasonCode(), s.createdAt(), s.updatedAt(), s.finalizedAt()
        );
    }
    private static PaymentQueryResponses.MoneyResponse money(PaymentProjectionViews.MoneyView v) { return new PaymentQueryResponses.MoneyResponse(v.amount(), v.currency()); }
    private static PaymentQueryResponses.MaskedAccountResponse account(PaymentProjectionViews.MaskedAccountView v) { return v == null ? null : new PaymentQueryResponses.MaskedAccountResponse(v.reference(), v.maskedValue()); }
    private static PaymentQueryResponses.BankingVerificationResponse banking(PaymentProjectionViews.BankingVerification v) { return v == null ? null : new PaymentQueryResponses.BankingVerificationResponse(v.verificationId(), v.outcome(), v.reasonCodes(), v.observedAt()); }
    private static PaymentQueryResponses.PostingResponse posting(PaymentProjectionViews.Posting v) { return v == null ? null : new PaymentQueryResponses.PostingResponse(v.bankPostingReference(), v.outcome(), v.observedAt()); }
    private static PaymentQueryResponses.TfjResponse tfj(PaymentProjectionViews.Tfj v) { return v == null ? null : new PaymentQueryResponses.TfjResponse(v.status(), v.businessDate(), v.confirmedAt()); }
    private PaymentQueryResponses.NotificationResponse notification(PaymentProjectionViews.Notification v) { return new PaymentQueryResponses.NotificationResponse(v.type(), v.status(), v.eventId(), v.lastAttemptAt()); }
    private static PaymentQueryResponses.ReversalResponse reversal(PaymentProjectionViews.Reversal v) { return v == null ? null : new PaymentQueryResponses.ReversalResponse(v.status(), v.reversalReference(), v.observedAt()); }
}
