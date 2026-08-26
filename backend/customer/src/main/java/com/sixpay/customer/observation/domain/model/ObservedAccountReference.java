package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Locale;
import java.util.regex.Pattern;

public record ObservedAccountReference(
        String accountBindingFingerprint,
        String maskedValue
) implements ValueObject {

    private static final Pattern FINGERPRINT = Pattern.compile(
            "^v1:[0-9a-f]{64}$"
    );

    public ObservedAccountReference {
        if (accountBindingFingerprint == null) {
            throw new ObservedCustomerDomainException(
                    "accountBindingFingerprint is required"
            );
        }
        accountBindingFingerprint = accountBindingFingerprint.strip();
        if (!FINGERPRINT.matcher(accountBindingFingerprint).matches()) {
            throw new ObservedCustomerDomainException(
                    "accountBindingFingerprint must match "
                            + "v1:<64 lowercase hexadecimal characters>"
            );
        }

        if (maskedValue == null) {
            throw new ObservedCustomerDomainException(
                    "masked account value is required"
            );
        }
        maskedValue = maskedValue.strip();
        if (maskedValue.isEmpty() || maskedValue.length() > 32) {
            throw new ObservedCustomerDomainException(
                    "masked account value has an invalid length"
            );
        }
        if (!containsMaskingMarker(maskedValue)) {
            throw new ObservedCustomerDomainException(
                    "masked account value must contain a masking marker"
            );
        }
    }

    public static ObservedAccountReference of(
            String accountBindingFingerprint,
            String maskedValue
    ) {
        return new ObservedAccountReference(
                accountBindingFingerprint,
                maskedValue
        );
    }

    private static boolean containsMaskingMarker(String value) {
        return value.indexOf('*') >= 0
                || value.indexOf('•') >= 0
                || value.indexOf('#') >= 0
                || value.toUpperCase(Locale.ROOT).contains("[MASKED]");
    }

    @Override
    public String toString() {
        return "ObservedAccountReference["
                + "accountBindingFingerprint=[PROTECTED], "
                + "maskedValue=[PROTECTED]]";
    }
}
