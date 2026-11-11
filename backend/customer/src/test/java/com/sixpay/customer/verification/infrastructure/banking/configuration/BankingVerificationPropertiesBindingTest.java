package com.sixpay.customer.verification.infrastructure.banking.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BankingVerificationPropertiesBindingTest {

    @Test
    void nestedContractBindsVersionAndAllCodes() {
        var properties = new MapConfigurationPropertySource(Map.of(
                "sixpay.customer.verification.banking.contract.version", "1.0.0",
                "sixpay.customer.verification.banking.contract.success-codes[0]", "00",
                "sixpay.customer.verification.banking.contract.business-failure-codes[0]", "01",
                "sixpay.customer.verification.banking.contract.business-failure-codes[1]", "02"
        ));

        var contract = new Binder(properties).bind(
                "sixpay.customer.verification.banking.contract",
                BankingVerificationProperties.Contract.class
        ).orElseThrow(() -> new IllegalStateException(
                "Banking contract binding failed"));

        assertNotNull(contract);
        assertEquals("1.0.0", contract.version());
        assertEquals(Set.of("00"), contract.successCodes());
        assertEquals(Set.of("01", "02"), contract.businessFailureCodes());
    }
}
