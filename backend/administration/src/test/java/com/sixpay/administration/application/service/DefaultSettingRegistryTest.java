package com.sixpay.administration.application.service;

import com.sixpay.administration.domain.model.SettingClassification;
import com.sixpay.administration.domain.model.SettingDomain;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSettingRegistryTest {

    private final DefaultSettingRegistry registry = new DefaultSettingRegistry();

    @Test
    void registryContainsApprovedOperationalFamiliesAndNoSecrets() {
        assertThat(registry.definitions()).isNotEmpty();
        assertThat(registry.definitions()).allSatisfy(definition -> {
            assertThat(definition.classification()).isEqualTo(SettingClassification.DYNAMIC_OPERATIONAL);
            assertThat(definition.dynamic()).isTrue();
            assertThat(definition.sensitive()).isFalse();
            assertThat(definition.key()).doesNotContainIgnoringCase("secret");
            assertThat(definition.key()).doesNotContainIgnoringCase("private-key");
            assertThat(definition.key()).doesNotContainIgnoringCase("hmac-key");
        });
    }

    @Test
    void registryKeepsDomainOwnershipExplicit() {
        assertThat(registry.find("payment.callback.max-attempts"))
                .hasValueSatisfying(d -> assertThat(d.domain()).isEqualTo(SettingDomain.PAYMENT));
        assertThat(registry.find("security.local.password.min-length"))
                .hasValueSatisfying(d -> assertThat(d.domain()).isEqualTo(SettingDomain.SECURITY));
        assertThat(registry.find("notification.operational.retry.max-attempts"))
                .hasValueSatisfying(d -> assertThat(d.domain()).isEqualTo(SettingDomain.NOTIFICATION));
    }

    @Test
    void registryRejectsUnknownAndProtectedKeysByAbsence() {
        assertThat(registry.find("database.password")).isEmpty();
        assertThat(registry.find("payment.banking.amplitude.posting.posting-path")).isEmpty();
        assertThat(registry.find("integration.kafka.topic.payment")).isEmpty();
    }
}
