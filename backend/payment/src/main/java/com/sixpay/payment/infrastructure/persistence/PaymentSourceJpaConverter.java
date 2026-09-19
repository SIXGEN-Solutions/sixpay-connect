package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.PaymentSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists PaymentSource using its stable scalar identifier.
 *
 * <p>This preserves the existing payment_source column representation while
 * allowing the domain type to remain provider-neutral.</p>
 */
@Converter
public final class PaymentSourceJpaConverter
        implements AttributeConverter<PaymentSource, String> {

    @Override
    public String convertToDatabaseColumn(PaymentSource attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public PaymentSource convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PaymentSource.of(dbData);
    }
}
