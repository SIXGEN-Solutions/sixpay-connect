package com.sixpay.payment.domain.model.evidence;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.ExternalSystem;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

final class EvidenceValueObjectRules {

    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final Pattern PRINTABLE_ASCII_NO_WHITESPACE =
            Pattern.compile("^[!-~]+$");
    private static final Pattern ACCOUNT_BINDING_FINGERPRINT =
            Pattern.compile("^v1:[0-9a-f]{64}$");

    private EvidenceValueObjectRules() {
        throw new IllegalStateException("Utility class");
    }

    static <T> T requireNonNull(T value, String label) {
        return Objects.requireNonNull(value, label + " must not be null");
    }

    static UUID requireNonNilUuid(UUID value, String label) {
        requireNonNull(value, label);
        if (NIL_UUID.equals(value)) {
            throw new IllegalArgumentException(label + " must not be the nil UUID");
        }
        return value;
    }

    static String requirePattern(
            String value,
            Pattern pattern,
            int minimumLength,
            int maximumLength,
            String label
    ) {
        String canonical = trimAsciiWhitespace(value, label);
        if (canonical.length() < minimumLength
                || canonical.length() > maximumLength) {
            throw new IllegalArgumentException(
                    label + " length must be between "
                            + minimumLength + " and " + maximumLength
            );
        }
        if (!pattern.matcher(canonical).matches()) {
            throw new IllegalArgumentException(label + " has an invalid format");
        }
        return canonical;
    }

    static String requireOpaque(
            String value,
            int minimumLength,
            int maximumLength,
            String label
    ) {
        String canonical = trimAsciiWhitespace(value, label);
        int length = canonical.codePointCount(0, canonical.length());
        if (length < minimumLength || length > maximumLength) {
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
            int minimumLength,
            int maximumLength,
            String label
    ) {
        String canonical = trimAsciiWhitespace(value, label);
        if (canonical.length() < minimumLength
                || canonical.length() > maximumLength
                || !PRINTABLE_ASCII_NO_WHITESPACE.matcher(canonical).matches()) {
            throw new IllegalArgumentException(
                    label + " must contain "
                            + minimumLength + " to " + maximumLength
                            + " printable ASCII characters without whitespace"
            );
        }
        return canonical;
    }

    static String requireAccountBindingFingerprint(String value) {
        return requirePattern(
                value,
                ACCOUNT_BINDING_FINGERPRINT,
                67,
                67,
                "Account binding fingerprint"
        );
    }

    static void requireCanonicalCorrelationId(CorrelationId correlationId) {
        requireNonNull(correlationId, "Correlation ID");
        String value = correlationId.value();
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Correlation ID must be a canonical UUID",
                    exception
            );
        }
        requireNonNilUuid(parsed, "Correlation ID");
        if (!parsed.toString().equals(value)) {
            throw new IllegalArgumentException(
                    "Correlation ID must use canonical lowercase UUID format"
            );
        }
    }

    static ExternalSystem requireSnapshotSource(ExternalSystem source) {
        requireNonNull(source, "Evidence source system");
        if (source == ExternalSystem.NOT_APPLICABLE) {
            throw new IllegalArgumentException(
                    "Snapshot source must identify a real system"
            );
        }
        return source;
    }

    static void requireNotBefore(
            Instant later,
            Instant earlier,
            String message
    ) {
        requireNonNull(later, "Later instant");
        requireNonNull(earlier, "Earlier instant");
        if (later.isBefore(earlier)) {
            throw new IllegalArgumentException(message);
        }
    }

    static String trimAsciiWhitespace(String value, String label) {
        requireNonNull(value, label);
        int start = 0;
        int end = value.length();
        while (start < end && isAsciiWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isAsciiWhitespace(value.charAt(end - 1))) {
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
