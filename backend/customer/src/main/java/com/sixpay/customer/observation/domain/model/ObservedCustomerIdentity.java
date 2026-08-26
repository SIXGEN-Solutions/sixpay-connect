package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record ObservedCustomerIdentity(
        String normalizedNiu,
        String legalName,
        String phoneMasked,
        String emailMasked
) implements ValueObject {

    private static final int MAX_NIU_LENGTH = 64;
    private static final int MAX_LEGAL_NAME_LENGTH = 200;
    private static final int MAX_MASKED_CONTACT_LENGTH = 128;
    private static final Pattern NIU_FORMAT = Pattern.compile(
            "^[A-Z0-9][A-Z0-9._/-]{0,63}$"
    );

    public ObservedCustomerIdentity {
        normalizedNiu = normalizeNiu(normalizedNiu);
        legalName = normalizeLegalName(legalName);
        phoneMasked = normalizeMasked(phoneMasked, "phoneMasked");
        emailMasked = normalizeMasked(emailMasked, "emailMasked");
    }

    public static ObservedCustomerIdentity of(
            String normalizedNiu,
            String legalName,
            String phoneMasked,
            String emailMasked
    ) {
        return new ObservedCustomerIdentity(
                normalizedNiu,
                legalName,
                phoneMasked,
                emailMasked
        );
    }

    public Optional<String> phoneMaskedOptional() {
        return Optional.ofNullable(phoneMasked);
    }

    public Optional<String> emailMaskedOptional() {
        return Optional.ofNullable(emailMasked);
    }

    static String normalizeNiu(String value) {
        if (value == null) {
            throw new ObservedCustomerDomainException(
                    "normalized NIU is required"
            );
        }
        String normalized = value.strip()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new ObservedCustomerDomainException(
                    "normalized NIU must not be blank"
            );
        }
        if (normalized.length() > MAX_NIU_LENGTH
                || !NIU_FORMAT.matcher(normalized).matches()) {
            throw new ObservedCustomerDomainException(
                    "normalized NIU has an invalid format"
            );
        }
        return normalized;
    }

    static String normalizeLegalName(String value) {
        if (value == null) {
            throw new ObservedCustomerDomainException(
                    "legalName is required"
            );
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.length() < 2
                || normalized.length() > MAX_LEGAL_NAME_LENGTH
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new ObservedCustomerDomainException(
                    "legalName has an invalid value"
            );
        }
        return normalized;
    }

    private static String normalizeMasked(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_MASKED_CONTACT_LENGTH
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new ObservedCustomerDomainException(
                    fieldName + " has an invalid value"
            );
        }
        if (!containsMaskingMarker(normalized)) {
            throw new ObservedCustomerDomainException(
                    fieldName + " must contain a masking marker"
            );
        }
        return normalized;
    }

    private static boolean containsMaskingMarker(String value) {
        return value.indexOf('*') >= 0
                || value.indexOf('•') >= 0
                || value.indexOf('#') >= 0
                || value.toUpperCase(Locale.ROOT).contains("[MASKED]");
    }

    @Override
    public String toString() {
        return "ObservedCustomerIdentity[normalizedNiu=[PROTECTED], "
                + "legalName=[PROTECTED], phoneMasked=[PROTECTED], "
                + "emailMasked=[PROTECTED]]";
    }
}
