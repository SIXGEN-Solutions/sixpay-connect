package com.sixpay.partner.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PartnerId(UUID value) {

    public PartnerId {
        Objects.requireNonNull(value, "value is required");
    }

    public static PartnerId from(String value) {
        return new PartnerId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
