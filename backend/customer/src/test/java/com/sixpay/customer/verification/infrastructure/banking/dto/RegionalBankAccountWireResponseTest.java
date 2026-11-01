package com.sixpay.customer.verification.infrastructure.banking.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegionalBankAccountWireResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void deserializesCompleteRegionalBankAccountWithoutInventingLegacyProperties() throws Exception {
        var account = mapper.readValue("""
                {
                  "ncp":"04942231051", "age":"00001", "dev":"001",
                  "cha":"10", "clc":"47", "cli":"0494223",
                  "inti":"TEST CUSTOMER", "ife":"I", "cfe":"C",
                  "dou":"2024-10-05", "sin":42.25, "typ":"CURRENT",
                  "sde":46.75, "suf":"00", "status":"ACTIVE"
                }
                """, RegionalBankAccountWireResponse.class);
        assertThat(account.accountReference()).isEqualTo("00001-04942231051-47");
        assertThat(account.cli()).isEqualTo("0494223");
        assertThat(account.dev()).isEqualTo("001");
        assertThat(account.typ()).isEqualTo("CURRENT");
        assertThat(account.dou().toString()).isEqualTo("2024-10-05");
        assertThat(account.sin().toPlainString()).isEqualTo("42.25");
        assertThat(account.sde().toPlainString()).isEqualTo("46.75");
        String json = mapper.writeValueAsString(account);
        assertThat(json).contains("\"ncp\"", "\"cli\"", "\"dev\"");
        assertThat(json).doesNotContain("maskedAccountIdentifier", "retrievedAt", "accountType");
    }

    @Test
    void missingAccountReferenceComponentsFailClosed() {
        var account = new RegionalBankAccountWireResponse(
                "123", "00001", "001", null, null, "C001", "A", null, null,
                null, null, null, null, null, "UNKNOWN");
        assertThatThrownBy(account::accountReference)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reference components");
    }
}
