package com.sixpay.integration.logging;

import java.util.regex.Pattern;

public final class SensitiveValueSanitizer {
    private static final Pattern BEARER =
            Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/-]+=*");
    private static final Pattern PRIVATE_KEY =
            Pattern.compile("(?s)-----BEGIN [^-]*PRIVATE KEY-----.*?-----END [^-]*PRIVATE KEY-----");
    private static final Pattern LONG_NUMBER =
            Pattern.compile("(?<!\\d)\\d{8,}(?!\\d)");

    public String sanitize(String value) {
        if (value == null) return null;
        String result = BEARER.matcher(value).replaceAll("Bearer [REDACTED]");
        result = PRIVATE_KEY.matcher(result).replaceAll("[PRIVATE_KEY_REDACTED]");
        return LONG_NUMBER.matcher(result).replaceAll(match -> mask(match.group()));
    }

    private static String mask(String value) {
        return "*".repeat(Math.max(4, value.length() - 4))
                + value.substring(value.length() - 4);
    }
}
