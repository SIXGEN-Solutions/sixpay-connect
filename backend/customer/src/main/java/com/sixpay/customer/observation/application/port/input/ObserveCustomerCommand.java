package com.sixpay.customer.observation.application.port.input;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.customer.observation.domain.model.ObservedAccountReference;
import com.sixpay.customer.observation.domain.model.ObservedCustomerIdentity;
import com.sixpay.customer.observation.domain.model.ObservedCustomerInstitution;
import com.sixpay.customer.observation.domain.model.ObservedPaymentReference;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import com.sixpay.customer.observation.domain.model.ProjectionWatermark;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer-owned command representing one Payment observation.
 *
 * <p>The command is independent of Payment classes. The {@code phone} and
 * {@code email} values, when present, must already be masked. The application
 * never masks or hashes them silently.</p>
 */
public record ObserveCustomerCommand(
        UUID sourceEventId,
        UUID paymentId,
        String paymentReference,
        String normalizedNiu,
        String legalName,
        String phone,
        String email,
        String financialInstitutionCode,
        String accountBindingFingerprint,
        String maskedAccountReference,
        BigDecimal amount,
        String currency,
        ObservedPaymentStatus paymentStatus,
        String failureReasonCode,
        Instant paymentCreatedAt,
        Instant paymentUpdatedAt,
        Instant observedAt,
        String correlationId
) {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObserveCustomerCommand {
        sourceEventId = requireUuid(
                sourceEventId,
                "sourceEventId"
        );
        paymentId = requireUuid(
                paymentId,
                "paymentId"
        );

        paymentReference = requireText(
                paymentReference,
                "paymentReference",
                128
        );

        paymentStatus = Objects.requireNonNull(
                paymentStatus,
                "paymentStatus is required"
        );

        amount = Objects.requireNonNull(
                amount,
                "amount is required"
        );

        paymentCreatedAt = Objects.requireNonNull(
                paymentCreatedAt,
                "paymentCreatedAt is required"
        );
        paymentUpdatedAt = Objects.requireNonNull(
                paymentUpdatedAt,
                "paymentUpdatedAt is required"
        );
        observedAt = Objects.requireNonNull(
                observedAt,
                "observedAt is required"
        );

        if (paymentUpdatedAt.isBefore(paymentCreatedAt)) {
            throw new ObservedCustomerDomainException(
                    "paymentUpdatedAt must not be before "
                            + "paymentCreatedAt"
            );
        }

        correlationId = requireCanonicalUuidText(
                correlationId,
                "correlationId"
        );

        /*
         * Reuse the domain value objects as the authoritative validation
         * rules. Their normalized values are then retained by the command.
         */
        ObservedCustomerIdentity identity =
                ObservedCustomerIdentity.of(
                        normalizedNiu,
                        legalName,
                        phone,
                        email
                );

        ObservedAccountReference accountReference =
                ObservedAccountReference.of(
                        accountBindingFingerprint,
                        maskedAccountReference
                );

        ObservedPaymentReference paymentReferenceValue =
                new ObservedPaymentReference(
                        paymentId,
                        paymentReference,
                        financialInstitutionCode,
                        amount,
                        currency,
                        paymentStatus,
                        failureReasonCode,
                        paymentCreatedAt,
                        paymentUpdatedAt
                );

        normalizedNiu = identity.normalizedNiu();
        legalName = identity.legalName();
        phone = identity.phoneMasked();
        email = identity.emailMasked();

        financialInstitutionCode =
                paymentReferenceValue.financialInstitutionCode();
        accountBindingFingerprint =
                accountReference.accountBindingFingerprint();
        maskedAccountReference =
                accountReference.maskedValue();
        currency = paymentReferenceValue.currency();
        failureReasonCode =
                paymentReferenceValue.failureReasonCode();
    }

    public ObservedCustomerIdentity identity() {
        return ObservedCustomerIdentity.of(
                normalizedNiu,
                legalName,
                phone,
                email
        );
    }

    public ObservedAccountReference accountReference() {
        return ObservedAccountReference.of(
                accountBindingFingerprint,
                maskedAccountReference
        );
    }

    public ObservedCustomerInstitution institution() {
        return ObservedCustomerInstitution.of(
                financialInstitutionCode,
                observedAt,
                observedAt,
                List.of(accountReference())
        );
    }

    public ObservedPaymentReference payment() {
        return new ObservedPaymentReference(
                paymentId,
                paymentReference,
                financialInstitutionCode,
                amount,
                currency,
                paymentStatus,
                failureReasonCode,
                paymentCreatedAt,
                paymentUpdatedAt
        );
    }

    public ProjectionWatermark watermark() {
        return ProjectionWatermark.of(
                sourceEventId.toString()
        );
    }

    private static UUID requireUuid(
            UUID value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " is required"
        );

        if (NIL_UUID.equals(value)) {
            throw new ObservedCustomerDomainException(
                    fieldName + " must not be nil"
            );
        }

        return value;
    }

    private static String requireText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null) {
            throw new ObservedCustomerDomainException(
                    fieldName + " is required"
            );
        }

        String normalized = value.strip();

        if (normalized.isEmpty()
                || normalized.length() > maxLength
                || normalized.chars()
                        .anyMatch(Character::isISOControl)) {
            throw new ObservedCustomerDomainException(
                    fieldName + " has an invalid value"
            );
        }

        return normalized;
    }

    private static String requireCanonicalUuidText(
            String value,
            String fieldName
    ) {
        String normalized = requireText(
                value,
                fieldName,
                36
        );

        try {
            UUID parsed = UUID.fromString(normalized);

            if (!parsed.toString().equals(normalized)) {
                throw new IllegalArgumentException(
                        "UUID is not canonical"
                );
            }

            if (NIL_UUID.equals(parsed)) {
                throw new IllegalArgumentException(
                        "UUID must not be nil"
                );
            }

            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new ObservedCustomerDomainException(
                    fieldName + " must be a canonical UUID",
                    exception
            );
        }
    }

    @Override
    public String toString() {
        return "ObserveCustomerCommand[sourceEventId="
                + sourceEventId
                + ", paymentId="
                + paymentId
                + ", paymentReference="
                + paymentReference
                + ", normalizedNiu=[PROTECTED]"
                + ", legalName=[PROTECTED]"
                + ", phone=[PROTECTED]"
                + ", email=[PROTECTED]"
                + ", financialInstitutionCode="
                + financialInstitutionCode
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", maskedAccountReference=[PROTECTED]"
                + ", amount="
                + amount
                + ", currency="
                + currency
                + ", paymentStatus="
                + paymentStatus
                + ", failureReasonCode="
                + failureReasonCode
                + ", paymentCreatedAt="
                + paymentCreatedAt
                + ", paymentUpdatedAt="
                + paymentUpdatedAt
                + ", observedAt="
                + observedAt
                + ", correlationId="
                + correlationId
                + "]";
    }
}
