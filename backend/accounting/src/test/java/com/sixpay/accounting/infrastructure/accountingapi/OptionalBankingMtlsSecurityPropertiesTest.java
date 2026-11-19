package com.sixpay.accounting.infrastructure.accountingapi;

import com.sixpay.accounting.infrastructure.accountingapi.configuration.AccountingApiProperties;
import com.sixpay.accounting.infrastructure.tfj.configuration.AccountingTfjLookupProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OptionalBankingMtlsSecurityPropertiesTest {
    @Test
    void accountingAndTfjDoNotRequireStoreWhenMtlsDisabled() {
        assertDoesNotThrow(() -> new AccountingApiProperties.Security(false, false, null));
        assertDoesNotThrow(() -> new AccountingTfjLookupProperties.Security(false, false, null));
    }

    @Test
    void enabledMtlsMustHaveStoreAndCannotAllowHttp() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountingApiProperties.Security(true, false, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AccountingTfjLookupProperties.Security(true, true, "banking"));
    }
}
