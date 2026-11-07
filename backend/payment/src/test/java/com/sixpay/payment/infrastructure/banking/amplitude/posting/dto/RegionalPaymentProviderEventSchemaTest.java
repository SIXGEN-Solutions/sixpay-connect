package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class RegionalPaymentProviderEventSchemaTest {
    @Test
    void declaresAllRegionalProviderProperties() {
        assertEquals(243, java.util.Arrays.stream(
                AmplitudePaymentProviderEvent.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .count());
    }

    @Test
    void serializesOnlyPopulatedBankingFields() throws Exception {
        var event = new AmplitudePaymentProviderEvent();
        event.setPaymentReference("PAY-EXAMPLE");
        event.setCurrency("001");
        event.setOperationCode("787");
        event.setTechnicalUser("001");
        var json = new JsonMapper().writeValueAsString(event);
        assertTrue(json.contains("paymentReference"));
        assertTrue(json.contains("currency"));
        assertFalse(json.contains("dco"));
        assertFalse(json.contains("eve"));
    }
}
