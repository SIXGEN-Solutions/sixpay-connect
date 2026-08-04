package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public record ProjectionWatermark(String value) implements ValueObject {

    private static final int MAX_LENGTH = 256;

    public ProjectionWatermark {
        if (value == null) {
            throw new ObservedCustomerDomainException(
                    "projection watermark is required"
            );
        }
        value = value.strip();
        if (value.isEmpty()) {
            throw new ObservedCustomerDomainException(
                    "projection watermark must not be blank"
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new ObservedCustomerDomainException(
                    "projection watermark must not exceed "
                            + MAX_LENGTH + " characters"
            );
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new ObservedCustomerDomainException(
                    "projection watermark must not contain control characters"
            );
        }
    }

    public static ProjectionWatermark of(String value) {
        return new ProjectionWatermark(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED_WATERMARK]";
    }
}
