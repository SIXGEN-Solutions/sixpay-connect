package com.sixpay.customer.observation.api.mapper;

import com.sixpay.customer.observation.api.dto.*;
import com.sixpay.customer.observation.application.query.*;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class ObservedCustomerQueryApiMapper {

    public ObservedCustomerSearchPageResponse toResponse(
            ObservedCustomerSearchPage page
    ) {
        Objects.requireNonNull(page, "page is required");

        return new ObservedCustomerSearchPageResponse(
                page.items().stream()
                        .map(this::toSummary)
                        .toList(),
                page.size(),
                page.hasMore(),
                page.nextCursor() == null
                        ? null
                        : page.nextCursor().value(),
                page.snapshotAt()
        );
    }

    public ObservedCustomerDetailResponse toResponse(
            ObservedCustomerDetailView view
    ) {
        Objects.requireNonNull(view, "view is required");

        return new ObservedCustomerDetailResponse(
                view.observedCustomerId().value(),
                masked(view.niu()),
                view.legalName(),
                masked(view.phone()),
                masked(view.email()),
                view.institutions().stream()
                        .map(this::toInstitution)
                        .toList(),
                view.firstObservedAt(),
                view.lastObservedAt(),
                view.totalPayments(),
                view.successfulPayments(),
                view.failedPayments(),
                name(view.lastPaymentStatus()),
                view.lastFailureReasonCode(),
                view.projectionUpdatedAt(),
                view.projectionVersion(),
                view.sourceEventWatermark()
        );
    }

    public ObservedCustomerPaymentPageResponse toResponse(
            ObservedCustomerPaymentPage page
    ) {
        Objects.requireNonNull(page, "page is required");

        return new ObservedCustomerPaymentPageResponse(
                page.items().stream()
                        .map(this::toPayment)
                        .toList(),
                page.size(),
                page.hasMore(),
                page.nextCursor() == null
                        ? null
                        : page.nextCursor().value(),
                page.snapshotAt()
        );
    }

    private ObservedCustomerSummaryResponse toSummary(
            ObservedCustomerSummaryView view
    ) {
        return new ObservedCustomerSummaryResponse(
                view.observedCustomerId().value(),
                masked(view.niu()),
                view.legalName(),
                masked(view.phone()),
                masked(view.email()),
                view.firstObservedAt(),
                view.lastObservedAt(),
                view.totalPayments(),
                view.successfulPayments(),
                view.failedPayments(),
                name(view.lastPaymentStatus()),
                view.lastFailureReasonCode(),
                view.projectionUpdatedAt(),
                view.projectionVersion()
        );
    }

    private InstitutionObservationResponse toInstitution(
            ObservedInstitutionView view
    ) {
        return new InstitutionObservationResponse(
                view.financialInstitutionCode(),
                view.firstObservedAt(),
                view.lastObservedAt(),
                view.accounts().stream()
                        .map(account ->
                                new MaskedAccountReferenceResponse(
                                        account.reference(),
                                        account.maskedValue()
                                )
                        )
                        .toList()
        );
    }

    private ObservedCustomerPaymentResponse toPayment(
            ObservedCustomerPaymentView view
    ) {
        return new ObservedCustomerPaymentResponse(
                view.paymentId(),
                view.paymentReference(),
                view.financialInstitutionCode(),
                view.amount(),
                view.currency(),
                view.status().name(),
                view.reasonCode(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private static MaskedIdentifierResponse masked(
            MaskedIdentifierView value
    ) {
        return value == null
                ? null
                : new MaskedIdentifierResponse(
                        value.maskedValue()
                );
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
