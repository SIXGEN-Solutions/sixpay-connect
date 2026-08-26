package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validated HTTPS callback endpoint associated with one Payment.
 *
 * <p>The domain validates only the canonical textual representation.
 * DNS resolution, network reachability, callback allow-list validation
 * and SSRF protection belong to the application or infrastructure layer.</p>
 *
 * @param value canonical HTTPS callback endpoint
 */
public record CallbackEndpoint(
        String value
) implements ValueObject {

    private static final int MAXIMUM_LENGTH = 2048;

    private static final Pattern HTTPS_ENDPOINT =
            Pattern.compile(
                    "^https://"
                            + "[A-Za-z0-9]"
                            + "(?:[A-Za-z0-9.-]*[A-Za-z0-9])?"
                            + "(?::[0-9]{1,5})?"
                            + "(?:/[^\\s#]*)?"
                            + "(?:\\?[^\\s#]*)?"
                            + "$"
            );

    public CallbackEndpoint {
        value = normalize(value);

        if (value.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException(
                    "Callback endpoint exceeds "
                            + MAXIMUM_LENGTH
                            + " characters"
            );
        }

        if (!HTTPS_ENDPOINT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Callback endpoint must be a valid HTTPS URL"
            );
        }

        if (containsUserInformation(value)) {
            throw new IllegalArgumentException(
                    "Callback endpoint must not contain user information"
            );
        }

        if (value.contains("#")) {
            throw new IllegalArgumentException(
                    "Callback endpoint must not contain a fragment"
            );
        }

        validatePort(value);
    }

    public static CallbackEndpoint of(String value) {
        return new CallbackEndpoint(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Callback endpoint must not be null"
            );
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Callback endpoint must not be blank"
            );
        }

        if (!normalized
                .toLowerCase(Locale.ROOT)
                .startsWith("https://")) {
            throw new IllegalArgumentException(
                    "Callback endpoint must use HTTPS"
            );
        }

        return normalized;
    }

    private static boolean containsUserInformation(
            String value
    ) {
        int authorityStart =
                "https://".length();

        int authorityEnd =
                firstPositiveIndex(
                        value,
                        authorityStart,
                        '/',
                        '?',
                        '#'
                );

        String authority = authorityEnd < 0
                ? value.substring(authorityStart)
                : value.substring(
                authorityStart,
                authorityEnd
        );

        return authority.contains("@");
    }

    private static void validatePort(String value) {
        int authorityStart =
                "https://".length();

        int authorityEnd =
                firstPositiveIndex(
                        value,
                        authorityStart,
                        '/',
                        '?',
                        '#'
                );

        String authority = authorityEnd < 0
                ? value.substring(authorityStart)
                : value.substring(
                authorityStart,
                authorityEnd
        );

        int colonIndex =
                authority.lastIndexOf(':');

        if (colonIndex < 0) {
            return;
        }

        String portValue =
                authority.substring(colonIndex + 1);

        try {
            int port = Integer.parseInt(portValue);

            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                        "Callback endpoint port must be "
                                + "between 1 and 65535"
                );
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Callback endpoint port is invalid",
                    exception
            );
        }
    }

    private static int firstPositiveIndex(
            String value,
            int fromIndex,
            char... candidates
    ) {
        int first = -1;

        for (char candidate : candidates) {
            int index =
                    value.indexOf(
                            candidate,
                            fromIndex
                    );

            if (index >= 0
                    && (first < 0 || index < first)) {
                first = index;
            }
        }

        return first;
    }

    @Override
    public String toString() {
        return value;
    }
}