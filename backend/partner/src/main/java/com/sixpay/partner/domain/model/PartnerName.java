package com.sixpay.partner.domain.model;

public record PartnerName(String value) {

    private static final int MAX_LENGTH = 200;

    public PartnerName {
        value = requireText(value, "legal name");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("legal name must not exceed " + MAX_LENGTH + " characters");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }
}
