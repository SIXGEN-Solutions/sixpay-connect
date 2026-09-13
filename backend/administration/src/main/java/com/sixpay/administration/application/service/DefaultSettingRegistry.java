package com.sixpay.administration.application.service;

import com.sixpay.administration.application.port.input.SettingRegistryQueryUseCase;
import com.sixpay.administration.domain.model.SettingClassification;
import com.sixpay.administration.domain.model.SettingDefinition;
import com.sixpay.administration.domain.model.SettingDomain;
import com.sixpay.administration.domain.model.SettingValueType;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public final class DefaultSettingRegistry implements SettingRegistryQueryUseCase {

    private final Map<String, SettingDefinition> definitions;

    public DefaultSettingRegistry() {
        Map<String, SettingDefinition> registry = new LinkedHashMap<>();

        add(registry, integer("security.local.password.min-length", SettingDomain.SECURITY, "12", "1", "200", "Minimum local password length"));
        add(registry, integer("security.local.password.max-length", SettingDomain.SECURITY, "200", "1", "1024", "Maximum local password length"));
        add(registry, integer("security.local.password.history-size", SettingDomain.SECURITY, "5", "0", "100", "Local password history depth"));
        add(registry, integer("security.local.password.expiration-days", SettingDomain.SECURITY, "90", "1", "3650", "Local password expiration period in days"));
        add(registry, integer("security.local.authentication.maximum-failed-attempts", SettingDomain.SECURITY, "5", "1", "100", "Failed local authentication attempts before lock"));
        add(registry, duration("security.local.authentication.lock-duration", SettingDomain.SECURITY, "PT15M", "PT1S", "P7D", "Local account lock duration"));
        add(registry, duration("security.local.session.timeout", SettingDomain.SECURITY, "PT30M", "PT1M", "P1D", "Local authenticated session timeout"));

        add(registry, duration("payment.tresorpay.anti-replay.allowed-clock-skew", SettingDomain.PAYMENT, "PT5M", "PT0S", "PT1H", "Accepted clock skew for TresorPay anti-replay"));
        add(registry, duration("payment.tresorpay.anti-replay.nonce-ttl", SettingDomain.PAYMENT, "PT10M", "PT1S", "P1D", "TresorPay nonce retention period"));
        add(registry, integer("payment.tresorpay.rate-limit.requests-per-minute", SettingDomain.PAYMENT, "120", "1", "1000000", "TresorPay requests allowed per minute"));
        add(registry, duration("payment.tresorpay.callback.delivery-expiration", SettingDomain.PAYMENT, "PT24H", "PT1M", "P30D", "TresorPay callback delivery expiration"));

        add(registry, bool("payment.callback.enabled", SettingDomain.PAYMENT, "false", "Payment callback worker operational switch"));
        add(registry, duration("payment.callback.poll-delay", SettingDomain.PAYMENT, "PT2S", "PT0.1S", "PT1H", "Payment callback polling delay"));
        add(registry, integer("payment.callback.batch-size", SettingDomain.PAYMENT, "50", "1", "10000", "Payment callback worker batch size"));
        add(registry, integer("payment.callback.max-attempts", SettingDomain.PAYMENT, "8", "1", "100", "Maximum callback delivery attempts"));
        add(registry, duration("payment.callback.claim-timeout", SettingDomain.PAYMENT, "PT5M", "PT1S", "P1D", "Callback delivery claim timeout"));
        add(registry, duration("payment.callback.initial-retry-delay", SettingDomain.PAYMENT, "PT10S", "PT0.1S", "P1D", "Initial callback retry delay"));
        add(registry, duration("payment.callback.maximum-retry-delay", SettingDomain.PAYMENT, "PT1H", "PT1S", "P7D", "Maximum callback retry delay"));

        add(registry, integer("payment.confirmation.resend.max-attempts", SettingDomain.PAYMENT, "3", "0", "100", "Maximum confirmation challenge resend attempts"));
        add(registry, duration("payment.confirmation.resend.cooldown", SettingDomain.PAYMENT, "PT3M", "PT0S", "P1D", "Cooldown before confirmation challenge resend"));
        add(registry, duration("payment.confirmation.post-verification.execution-window", SettingDomain.PAYMENT, "PT1M", "PT1S", "PT1H", "Execution window after successful confirmation"));

        addProviderTimeouts(registry, "confirmation", "PT10S");
        addProviderTimeouts(registry, "reservation", "PT5S");
        addProviderTimeouts(registry, "posting", "PT10S");
        addProviderTimeouts(registry, "compensation", "PT10S");
        addProviderTimeouts(registry, "status", "PT5S");

        add(registry, duration("customer.verification.banking.connect-timeout", SettingDomain.CUSTOMER, "PT2S", "PT0.1S", "PT1M", "Core Banking customer verification connect timeout"));
        add(registry, duration("customer.verification.banking.read-timeout", SettingDomain.CUSTOMER, "PT5S", "PT0.1S", "PT5M", "Core Banking customer verification read timeout"));
        add(registry, integer("customer.verification.banking.max-attempts", SettingDomain.CUSTOMER, "3", "1", "100", "Core Banking customer verification maximum attempts"));
        add(registry, duration("customer.verification.banking.retry-backoff", SettingDomain.CUSTOMER, "PT0.25S", "PT0S", "PT1M", "Core Banking customer verification retry backoff"));
        add(registry, duration("customer.verification.banking.evidence-ttl", SettingDomain.CUSTOMER, "PT5M", "PT1S", "P1D", "Customer verification evidence TTL"));

        add(registry, integer("customer.observation.resilience.max-attempts", SettingDomain.CUSTOMER, "3", "1", "100", "Customer observation resilience maximum attempts"));
        add(registry, duration("customer.observation.resilience.initial-backoff", SettingDomain.CUSTOMER, "PT0.01S", "PT0S", "PT1M", "Customer observation initial backoff"));
        add(registry, duration("customer.observation.resilience.max-backoff", SettingDomain.CUSTOMER, "PT0.25S", "PT0S", "PT1H", "Customer observation maximum backoff"));
        add(registry, decimal("customer.observation.resilience.multiplier", SettingDomain.CUSTOMER, "2", "1", "100", "Customer observation backoff multiplier"));
        add(registry, decimal("customer.observation.resilience.jitter", SettingDomain.CUSTOMER, "0.20", "0", "1", "Customer observation retry jitter"));

        add(registry, bool("notification.operational.retry.enabled", SettingDomain.NOTIFICATION, "true", "Notification operational retry switch"));
        add(registry, integer("notification.operational.retry.max-attempts", SettingDomain.NOTIFICATION, "5", "1", "100", "Notification maximum retry attempts"));
        add(registry, duration("notification.operational.retry.initial-backoff", SettingDomain.NOTIFICATION, "PT30S", "PT0S", "P1D", "Notification initial retry backoff"));
        add(registry, duration("notification.operational.retry.max-backoff", SettingDomain.NOTIFICATION, "PT15M", "PT0S", "P7D", "Notification maximum retry backoff"));
        add(registry, integer("notification.operational.retry.batch-size", SettingDomain.NOTIFICATION, "50", "1", "10000", "Notification retry batch size"));
        add(registry, integer("notification.operational.retry.poll-interval-ms", SettingDomain.NOTIFICATION, "30000", "100", "86400000", "Notification retry polling interval in milliseconds"));
        add(registry, bool("notification.operational.email.enabled", SettingDomain.NOTIFICATION, "false", "Operational email notification switch"));
        add(registry, string("notification.operational.email.subject-prefix", SettingDomain.NOTIFICATION, "[SIXPAY]", "0", "128", "Operational email subject prefix"));
        add(registry, integer("notification.operational.metrics.refresh-ms", SettingDomain.NOTIFICATION, "30000", "100", "86400000", "Notification metrics refresh interval"));
        add(registry, duration("notification.operational.retention.delivered", SettingDomain.NOTIFICATION, "P90D", "P1D", "P3650D", "Delivered notification retention"));
        add(registry, duration("notification.operational.retention.failed", SettingDomain.NOTIFICATION, "P365D", "P1D", "P3650D", "Failed notification retention"));
        add(registry, integer("notification.operational.purge.batch-size", SettingDomain.NOTIFICATION, "500", "1", "100000", "Notification purge batch size"));
        add(registry, integer("notification.operational.purge.interval-ms", SettingDomain.NOTIFICATION, "86400000", "1000", "604800000", "Notification purge interval in milliseconds"));

        add(registry, duration("accounting.api.connect-timeout", SettingDomain.ACCOUNTING, "PT2S", "PT0.1S", "PT1M", "Accounting API connect timeout"));
        add(registry, duration("accounting.api.read-timeout", SettingDomain.ACCOUNTING, "PT5S", "PT0.1S", "PT5M", "Accounting API read timeout"));
        add(registry, duration("accounting.tresorpay-status.connect-timeout", SettingDomain.ACCOUNTING, "PT2S", "PT0.1S", "PT1M", "TresorPay status connect timeout"));
        add(registry, duration("accounting.tresorpay-status.read-timeout", SettingDomain.ACCOUNTING, "PT5S", "PT0.1S", "PT5M", "TresorPay status read timeout"));

        add(registry, duration("reporting.audit-export.retention", SettingDomain.REPORTING, "PT1H", "PT1M", "P3650D", "Audit export retention"));
        add(registry, integer("reporting.audit-export.recovery-delay-ms", SettingDomain.REPORTING, "60000", "100", "86400000", "Audit export recovery delay in milliseconds"));

        add(registry, integer("integration.kafka.consumer.concurrency", SettingDomain.INTEGRATION, "1", "1", "1024", "Kafka consumer concurrency"));
        add(registry, integer("integration.kafka.outbox.batch-size", SettingDomain.INTEGRATION, "100", "1", "100000", "Outbox publication batch size"));
        add(registry, duration("integration.kafka.outbox.poll-interval", SettingDomain.INTEGRATION, "PT1S", "PT0.1S", "PT1H", "Outbox polling interval"));
        add(registry, duration("integration.kafka.outbox.delivered-retention", SettingDomain.INTEGRATION, "P30D", "P1D", "P3650D", "Delivered outbox retention"));
        add(registry, integer("integration.kafka.outbox.cleanup-batch-size", SettingDomain.INTEGRATION, "500", "1", "100000", "Outbox cleanup batch size"));

        definitions = Map.copyOf(registry);
    }

    @Override
    public Collection<SettingDefinition> definitions() {
        return definitions.values();
    }

    @Override
    public Optional<SettingDefinition> find(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        return Optional.ofNullable(definitions.get(key.strip()));
    }

    private static void addProviderTimeouts(Map<String, SettingDefinition> registry, String capability, String readTimeout) {
        add(registry, duration("payment.banking.amplitude." + capability + ".connect-timeout", SettingDomain.PAYMENT, "PT2S", "PT0.1S", "PT1M", "Amplitude " + capability + " connect timeout"));
        add(registry, duration("payment.banking.amplitude." + capability + ".read-timeout", SettingDomain.PAYMENT, readTimeout, "PT0.1S", "PT5M", "Amplitude " + capability + " read timeout"));
    }

    private static SettingDefinition bool(String key, SettingDomain domain, String defaultValue, String description) {
        return definition(key, domain, SettingValueType.BOOLEAN, defaultValue, null, null, Set.of("true", "false"), description);
    }

    private static SettingDefinition integer(String key, SettingDomain domain, String defaultValue, String minimum, String maximum, String description) {
        return definition(key, domain, SettingValueType.INTEGER, defaultValue, minimum, maximum, Set.of(), description);
    }

    private static SettingDefinition decimal(String key, SettingDomain domain, String defaultValue, String minimum, String maximum, String description) {
        return definition(key, domain, SettingValueType.DECIMAL, defaultValue, minimum, maximum, Set.of(), description);
    }

    private static SettingDefinition duration(String key, SettingDomain domain, String defaultValue, String minimum, String maximum, String description) {
        return definition(key, domain, SettingValueType.DURATION, defaultValue, minimum, maximum, Set.of(), description);
    }

    private static SettingDefinition string(String key, SettingDomain domain, String defaultValue, String minimum, String maximum, String description) {
        return definition(key, domain, SettingValueType.STRING, defaultValue, minimum, maximum, Set.of(), description);
    }

    private static SettingDefinition definition(
            String key, SettingDomain domain, SettingValueType type, String defaultValue,
            String minimum, String maximum, Set<String> allowedValues, String description) {
        return new SettingDefinition(
                key, domain, type, SettingClassification.DYNAMIC_OPERATIONAL,
                defaultValue, minimum, maximum, allowedValues, description,
                true, false, false, domain.name().toLowerCase() + "-validator");
    }

    private static void add(Map<String, SettingDefinition> registry, SettingDefinition definition) {
        if (registry.putIfAbsent(definition.key(), definition) != null) {
            throw new IllegalStateException("Duplicate dynamic-setting key: " + definition.key());
        }
    }
}
