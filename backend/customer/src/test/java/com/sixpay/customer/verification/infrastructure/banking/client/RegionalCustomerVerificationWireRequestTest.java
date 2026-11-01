package com.sixpay.customer.verification.infrastructure.banking.client;

import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeAccountVerificationSubject;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationRequest;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationSubject;
import com.sixpay.customer.verification.infrastructure.banking.dto.RegionalCustomerVerificationWireRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegionalCustomerVerificationWireRequestTest {
    @Test
    void serializesApprovedRegionalTopLevelFieldsWithoutLegacyNestedObjects() throws Exception {
        var request = new AmplitudeCustomerVerificationRequest(
                "REGIONAL",
                new AmplitudeCustomerVerificationSubject(null, "0494223", "P02551760811Z",
                        "Bank Account Holder", null, null),
                new AmplitudeAccountVerificationSubject("00001-04942231051-47", null, null),
                List.of("niu", "legalName"),
                Instant.parse("2026-11-01T03:10:00Z")
        );
        var wire = RegionalCustomerVerificationWireRequest.from(request);
        var json = JsonMapper.builder().build().writeValueAsString(wire);
        var node = JsonMapper.builder().build().readTree(json);
        assertEquals("00001-04942231051-47", node.get("accountReference").asText());
        assertEquals("P02551760811Z", node.get("expectedNiu").asText());
        assertEquals("Bank Account Holder", node.get("expectedAccountHolder").asText());
        assertTrue(node.has("requestedAt"));
        assertEquals(2, node.get("requiredKycFields").size());
        assertFalse(node.has("customer"));
        assertFalse(node.has("account"));
        assertFalse(node.has("financialInstitutionCode"));
    }
}
