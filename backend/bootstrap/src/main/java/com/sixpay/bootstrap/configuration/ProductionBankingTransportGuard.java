package com.sixpay.bootstrap.configuration;

import java.net.URI;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Ensures production never starts with an insecure Regional banking transport. */
@Component
@Profile("production")
public final class ProductionBankingTransportGuard {

    private static final String BANKING_URL = "sixpay.payment.banking.amplitude.";

    private static final List<String> INSECURE_HTTP_FLAGS = List.of(
            "AMPLITUDE_FUNDS_RESERVATION_ALLOW_INSECURE_HTTP",
            "AMPLITUDE_POSTING_ALLOW_INSECURE_HTTP",
            "AMPLITUDE_COMPENSATION_ALLOW_INSECURE_HTTP",
            "AMPLITUDE_CONFIRMATION_ALLOW_INSECURE_HTTP",
            "ACCOUNTING_API_ALLOW_INSECURE_HTTP",
            "AMPLITUDE_TFJ_ALLOW_INSECURE_HTTP");

    public ProductionBankingTransportGuard(Environment environment) {
        if (!environment.getProperty("SIXPAY_BANKING_MTLS_ENABLED", Boolean.class, false)) {
            throw new IllegalStateException(
                    "Production requires SIXPAY_BANKING_MTLS_ENABLED=true");
        }
        for (String name : INSECURE_HTTP_FLAGS) {
            if (environment.getProperty(name, Boolean.class, false)) {
                throw new IllegalStateException(
                        "Production forbids insecure banking HTTP: " + name);
            }
        }

        requireHttpsWhenEnabled(environment, BANKING_URL + "reservation", true);
        requireHttpsWhenEnabled(environment, BANKING_URL + "posting", true);
        requireHttpsWhenEnabled(environment, BANKING_URL + "compensation", false);
        requireHttpsWhenEnabled(environment, BANKING_URL + "status", false);
        requireHttpsWhenEnabled(environment, BANKING_URL + "confirmation", true);
        requireHttpsWhenEnabled(environment, "sixpay.customer.verification.banking", true);
        requireHttpsWhenEnabled(environment, "sixpay.accounting.api", false);
        requireHttpsWhenEnabled(environment, "sixpay.accounting.tfj.lookup", false);
    }

    private static void requireHttpsWhenEnabled(
            Environment environment, String prefix, boolean enabledByDefault) {
        if (!environment.getProperty(prefix + ".enabled", Boolean.class, enabledByDefault)) {
            return;
        }
        String raw = environment.getProperty(prefix + ".base-url");
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "Production banking URL is missing: " + prefix + ".base-url");
        }
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Production banking URL is invalid: " + prefix + ".base-url", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalStateException(
                    "Production banking URL must use HTTPS: " + prefix + ".base-url");
        }
    }
}
