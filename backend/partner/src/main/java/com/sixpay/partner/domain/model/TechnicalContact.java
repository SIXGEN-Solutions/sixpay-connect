package com.sixpay.partner.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record TechnicalContact(String name, String email) {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$", Pattern.CASE_INSENSITIVE);

    public TechnicalContact {
        name = requireText(name, "technical contact name");
        email = requireText(email, "technical contact email").toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("technical contact email is invalid");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
