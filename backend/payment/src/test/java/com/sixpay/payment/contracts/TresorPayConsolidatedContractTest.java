package com.sixpay.payment.contracts;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TresorPayConsolidatedContractTest {

    @Test
    void packagedPaymentContractDeclaresFinalSecurityAndErrors() {
        try (InputStream input = getClass().getResourceAsStream(
                "/openapi/payment-command-api-v1.yaml"
        )) {
            assertThat(input).isNotNull();
            Map<String, Object> contract = new Yaml().load(input);
            assertThat(contract.get("openapi")).isEqualTo("3.1.0");
            assertThat(contract).containsKeys(
                    "security",
                    "paths",
                    "components"
            );
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
