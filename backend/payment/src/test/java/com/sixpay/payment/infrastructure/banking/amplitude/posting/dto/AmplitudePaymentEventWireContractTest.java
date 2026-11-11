package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AmplitudePaymentEventWireContractTest {
    @Test
    void serializesT0WithoutProviderEntriesOrBankOwnedContext() throws Exception {
        var payload = new AmplitudePaymentEventRequest("PAY-TEST", "v1",
                new AmplitudePaymentProviderEvent("PAY-TEST", "787", "001",
                        "VIRPAY", "001", "00001-123456-42", "00001-999999-17",
                        new BigDecimal("1000"), new BigDecimal("1000"), "PAIEMENT/PAY-TEST"),
                Instant.parse("2026-11-05T12:00:00Z"));
        String json = new JsonMapper().writeValueAsString(payload);
        assertTrue(json.contains("\"providerEvent\""));
        assertFalse(json.contains("providerEntries"));
        assertFalse(json.contains("eventNumber"));
        assertFalse(json.contains("accountingDate"));
        assertFalse(json.contains("nightMode"));
    }
    @Test
    void keepsBankDcoAsAccountingDate() {
        var result = new AmplitudePaymentEventResult("PAY-TEST", "COMPLETED",
                java.util.List.of(), "BK-1", null,
                Instant.parse("2026-11-05T12:00:00Z"), LocalDate.of(2026, 11, 5));
        assertEquals(LocalDate.of(2026, 11, 5), result.accountingDate());
    }
}
