package com.sixpay.payment.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

final class PaymentValueObjectRules {

    private static final UUID NIL_UUID =
            new UUID(0L, 0L);

    private static final Pattern PRINTABLE_ASCII_NO_WHITESPACE =
            Pattern.compile("^[!-~]+$");

    private static final Pattern SAFE_MESSAGE_FORBIDDEN =
            Pattern.compile(
                    "(?i)(authorization\\s*:|bearer\\s+|password\\s*=|"
                            + "secret\\s*=|token\\s*=|account\\s*=|"
                            + "stack\\s*trace|\\bat\\s+[a-z0-9_.$]+\\([^)]*:"
                            + "\\d+\\))"
            );

    private PaymentValueObjectRules() {
        throw new IllegalStateException("Utility class");
    }

    static <T> T requireNonNull(
            T value,
            String label
    ) {
        return Objects.requireNonNull(
                value,
                label + " must not be null"
        );
    }

    static UUID requireNonNilUuid(
            UUID value,
            String label
    ) {
        requireNonNull(value, label);

        if (NIL_UUID.equals(value)) {
            throw new IllegalArgumentException(
                    label + " must not be the nil UUID"
            );
        }

        return value;
    }

    static UUID parseCanonicalUuid(
            String value,
            String label
    ) {
        String canonical = trimAsciiWhitespace(value, label);
        UUID parsed;

        try {
            parsed = UUID.fromString(canonical);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    label + " must be a canonical UUID",
                    exception
            );
        }

        requireNonNilUuid(parsed, label);

        if (!parsed.toString().equals(canonical)) {
            throw new IllegalArgumentException(
                    label + " must use canonical lowercase UUID format"
            );
        }

        return parsed;
    }

    static String requirePattern(
            String value,
            Pattern pattern,
            int minimumLength,
            int maximumLength,
            String label
    ) {
        String canonical = trimAsciiWhitespace(value, label);
        int length = canonical.length();

        if (length < minimumLength || length > maximumLength) {
            throw new IllegalArgumentException(
                    label + " length must be between "
                            + minimumLength + " and " + maximumLength
            );
        }

        if (!pattern.matcher(canonical).matches()) {
            throw new IllegalArgumentException(
                    label + " has an invalid format"
            );
        }

        return canonical;
    }

    static String requireUppercasePattern(
            String value,
            Pattern pattern,
            int minimumLength,
            int maximumLength,
            String label
    ) {
        return requirePattern(
                trimAsciiWhitespace(value, label)
                        .toUpperCase(Locale.ROOT),
                pattern,
                minimumLength,
                maximumLength,
                label
        );
    }

    static String requireOpaque(
            String value,
            int minimumLength,
            int maximumLength,
            String label
    ) {
        String canonical = trimAsciiWhitespace(value, label);
        int codePoints = canonical.codePointCount(
                0,
                canonical.length()
        );

        if (codePoints < minimumLength || codePoints > maximumLength) {
            throw new IllegalArgumentException(
                    label + " length must be between "
                            + minimumLength + " and " + maximumLength
            );
        }

        canonical.codePoints().forEach(codePoint -> {
            if (Character.isISOControl(codePoint)) {
                throw new IllegalArgumentException(
                        label + " must not contain control characters"
                );
            }
        });

        return canonical;
    }

    static String requirePrintableAsciiNoWhitespace(
            String value,
            int maximumLength,
            String label
    ) {
        String canonical = trimAsciiWhitespace(value, label);

        if (canonical.length() > maximumLength
                || !PRINTABLE_ASCII_NO_WHITESPACE
                .matcher(canonical)
                .matches()) {
            throw new IllegalArgumentException(
                    label + " must contain 1 to "
                            + maximumLength
                            + " printable ASCII characters without whitespace"
            );
        }

        return canonical;
    }

    static String requireMaskedDisplay(
            String value,
            String protectedValue,
            String label
    ) {
        String canonical = requireOpaque(
                value,
                2,
                128,
                label
        );

        if (!canonical.contains("*")) {
            throw new IllegalArgumentException(
                    label + " must contain masking characters"
            );
        }

        if (canonical.equals(protectedValue)) {
            throw new IllegalArgumentException(
                    label + " must not expose the protected value"
            );
        }

        return canonical;
    }

    static String requireSafeMessage(
            String value
    ) {
        String canonical = requireOpaque(
                value,
                1,
                300,
                "Safe message"
        );

        if (SAFE_MESSAGE_FORBIDDEN
                .matcher(canonical)
                .find()) {
            throw new IllegalArgumentException(
                    "Safe message contains forbidden sensitive "
                            + "or diagnostic material"
            );
        }

        return canonical;
    }

    static String trimAsciiWhitespace(
            String value,
            String label
    ) {
        requireNonNull(value, label);

        int start = 0;
        int end = value.length();

        while (start < end
                && isAsciiWhitespace(value.charAt(start))) {
            start++;
        }

        while (end > start
                && isAsciiWhitespace(value.charAt(end - 1))) {
            end--;
        }

        return value.substring(start, end);
    }

    private static boolean isAsciiWhitespace(char value) {
        return value == ' '
                || value == '\t'
                || value == '\n'
                || value == '\r'
                || value == '\f'
                || value == '\u000B';
    }
}
