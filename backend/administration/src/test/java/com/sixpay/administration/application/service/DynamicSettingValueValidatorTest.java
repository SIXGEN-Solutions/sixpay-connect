package com.sixpay.administration.application.service;

import com.sixpay.administration.domain.model.SettingClassification;
import com.sixpay.administration.domain.model.SettingDefinition;
import com.sixpay.administration.domain.model.SettingDomain;
import com.sixpay.administration.domain.model.SettingValueType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicSettingValueValidatorTest {

    private final DynamicSettingValueValidator validator = new DynamicSettingValueValidator();
    private final DefaultSettingRegistry registry = new DefaultSettingRegistry();

    @Test
    void acceptsValidRegisteredInteger() {
        validator.validate(registry.find("payment.callback.max-attempts").orElseThrow(), "10");
    }

    @Test
    void rejectsOutOfRangeRegisteredInteger() {
        assertThatThrownBy(() -> validator.validate(
                registry.find("payment.callback.max-attempts").orElseThrow(), "0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsIsoDurationAndRejectsMalformedDuration() {
        validator.validate(
                registry.find("security.local.authentication.lock-duration").orElseThrow(), "PT20M");
        assertThatThrownBy(() -> validator.validate(
                registry.find("security.local.authentication.lock-duration").orElseThrow(), "20 minutes"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesNonDynamicDefinitions() {
        SettingDefinition deployment = new SettingDefinition(
                "global.datasource.url",
                SettingDomain.GLOBAL,
                SettingValueType.STRING,
                SettingClassification.DEPLOYMENT_CONFIG,
                "jdbc:postgresql://localhost/sixpay",
                null,
                null,
                Set.of(),
                "Datasource URL",
                false,
                false,
                true,
                null);

        assertThatThrownBy(() -> validator.validate(deployment, "jdbc:postgresql://other/sixpay"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not dynamically mutable");
    }
}
