package com.sixpay.customer.observation.infrastructure.query.mapper;

import com.sixpay.customer.observation.application.query
        .MaskedIdentifierView;
import com.sixpay.customer.observation.application.query
        .ObservedAccountView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSummaryView;
import com.sixpay.customer.observation.application.query
        .ObservedInstitutionView;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import com.sixpay.customer.observation.infrastructure.persistence.protection
        .ObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedCustomerDetailRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedCustomerSummaryRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedInstitutionRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedPaymentRow;

import java.util.List;
import java.util.Objects;

/**
 * Maps infrastructure-only rows to Customer-owned immutable query views.
 */
public final class ObservedCustomerQueryRowMapper {

    private final ObservedCustomerDataProtector protector;

    public ObservedCustomerQueryRowMapper(
            ObservedCustomerDataProtector protector
    ) {
        this.protector = Objects.requireNonNull(
                protector,
                "protector is required"
        );
    }

    public ObservedCustomerSummaryView toSummary(
            ObservedCustomerSummaryRow row
    ) {
        Objects.requireNonNull(row, "row is required");

        String niu = protector.reveal(
                row.niuProtected()
        );
        String legalName = protector.reveal(
                row.legalNameProtected()
        );

        return new ObservedCustomerSummaryView(
                ObservedCustomerId.of(
                        row.observedCustomerId()
                ),
                new MaskedIdentifierView(
                        maskNiu(niu)
                ),
                legalName,
                maskedIdentifier(row.phoneMasked()),
                maskedIdentifier(row.emailMasked()),
                row.firstObservedAt(),
                row.lastObservedAt(),
                row.totalPayments(),
                row.successfulPayments(),
                row.failedPayments(),
                ObservedPaymentStatus.valueOf(
                        row.lastPaymentStatus()
                ),
                row.lastFailureReasonCode(),
                row.updatedAt(),
                row.projectionVersion()
        );
    }

    public ObservedCustomerDetailView toDetail(
            ObservedCustomerDetailRow row
    ) {
        Objects.requireNonNull(row, "row is required");

        String niu = protector.reveal(
                row.niuProtected()
        );
        String legalName = protector.reveal(
                row.legalNameProtected()
        );

        return new ObservedCustomerDetailView(
                ObservedCustomerId.of(
                        row.observedCustomerId()
                ),
                new MaskedIdentifierView(
                        maskNiu(niu)
                ),
                legalName,
                maskedIdentifier(row.phoneMasked()),
                maskedIdentifier(row.emailMasked()),
                row.institutions()
                        .stream()
                        .map(this::toInstitution)
                        .toList(),
                row.firstObservedAt(),
                row.lastObservedAt(),
                row.totalPayments(),
                row.successfulPayments(),
                row.failedPayments(),
                ObservedPaymentStatus.valueOf(
                        row.lastPaymentStatus()
                ),
                row.lastFailureReasonCode(),
                row.updatedAt(),
                row.projectionVersion(),
                row.sourceEventWatermark()
        );
    }

    public ObservedCustomerPaymentView toPayment(
            ObservedPaymentRow row
    ) {
        Objects.requireNonNull(row, "row is required");

        return new ObservedCustomerPaymentView(
                row.paymentId(),
                row.publicPaymentReference(),
                row.financialInstitutionCode(),
                row.amount(),
                row.currency(),
                ObservedPaymentStatus.valueOf(
                        row.paymentStatus()
                ),
                row.failureReasonCode(),
                row.paymentCreatedAt(),
                row.paymentUpdatedAt()
        );
    }

    private ObservedInstitutionView toInstitution(
            ObservedInstitutionRow row
    ) {
        List<ObservedAccountView> accounts =
                row.accounts()
                        .stream()
                        .map(account ->
                                new ObservedAccountView(
                                        account.observedAccountId()
                                                .toString(),
                                        account.maskedValue()
                                )
                        )
                        .toList();

        return new ObservedInstitutionView(
                row.financialInstitutionCode(),
                row.firstObservedAt(),
                row.lastObservedAt(),
                accounts
        );
    }

    private static MaskedIdentifierView maskedIdentifier(
            String value
    ) {
        return value == null
                ? null
                : new MaskedIdentifierView(value);
    }

    private static String maskNiu(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "revealed NIU must not be blank"
            );
        }

        String normalized = value.strip();

        if (normalized.length() <= 4) {
            return "*".repeat(normalized.length());
        }

        return "*".repeat(
                normalized.length() - 4
        ) + normalized.substring(
                normalized.length() - 4
        );
    }
}
