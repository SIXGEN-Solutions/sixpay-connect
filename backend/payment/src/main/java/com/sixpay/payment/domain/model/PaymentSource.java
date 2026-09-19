package com.sixpay.payment.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * External origin of the original Payment intention.
 *
 * <p>The Payment domain deliberately does not enumerate external partners.
 * Provider-specific boundaries supply stable opaque source identifiers without
 * changing this domain type.</p>
 */
public record PaymentSource(String value) implements ValueObject {

    private static final Pattern FORMAT =
            Pattern.compile("^[A-Z0-9][A-Z0-9_-]{1,31}$");

    public PaymentSource {
        value = Objects.requireNonNull(value, "Payment source")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Payment source must contain 2 to 32 uppercase "
                            + "letters, digits, '_' or '-'"
            );
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentSource of(String value) {
        return new PaymentSource(value);
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
