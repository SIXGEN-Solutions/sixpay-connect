package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import com.sixpay.payment.application.port.output.PaymentTreasuryAccountResolutionPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.TreasuryAccountReference;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Deterministic, fail-closed (institution, claim type) Treasury resolution. */
public final class ConfiguredTreasuryAccountResolutionAdapter
        implements PaymentTreasuryAccountResolutionPort {

    private static final Pattern ACCOUNT = Pattern.compile(
            "^[0-9]+-[0-9]+-[0-9]+$"
    );
    private final TreasuryResolutionProperties properties;
    private final TreasuryAccountLookupClient client;

    public ConfiguredTreasuryAccountResolutionAdapter(
            TreasuryResolutionProperties properties,
            TreasuryAccountLookupClient client
    ) {
        this.properties = Objects.requireNonNull(properties);
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public TreasuryAccountReference resolve(Payment payment, Instant requestedAt) {
        Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(requestedAt, "requestedAt");
        if (!properties.enabled()) {
            throw new IllegalStateException("Treasury account resolution is disabled");
        }
        var state = payment.toState();
        String institution = state.financialInstitutionCode().value();
        String claimType = state.initiationContext()
                .orElseThrow(() -> new IllegalStateException(
                        "Treasury resolution requires initiation context"))
                .claimType().name();
        var byType = findInstitution(properties.accounts(), institution);
        TreasuryResolutionProperties.Account configured = findClaim(byType, claimType);
        if (configured == null || blank(configured.configurationId())
                || blank(configured.version()) || blank(configured.accountReference())) {
            throw new IllegalStateException("Treasury configuration is missing or incomplete");
        }
        String reference = configured.accountReference();
        if (!ACCOUNT.matcher(reference).matches()) {
            throw new IllegalStateException("Treasury account reference has invalid format");
        }
        String[] parts = reference.split("-", -1);
        var response = client.lookup(parts[0], parts[1],
                state.requestIdentity().correlationId());
        if (response == null || response.code() == null) {
            throw new IllegalStateException("Treasury bank response is missing");
        }
        if ("COMPTE_INEXISTANT".equals(response.code())) {
            if (response.data() != null) {
                throw new IllegalStateException("Treasury bank response is contradictory");
            }
            throw new TreasuryAccountNotFoundException();
        }
        if (!"SUCCESS".equals(response.code()) || response.data() == null) {
            throw new IllegalStateException("Treasury bank response is invalid");
        }
        var data = response.data();
        if (!parts[0].equals(data.age()) || !parts[1].equals(data.ncp())
                || !parts[2].equals(data.clc())) {
            throw new IllegalStateException("Treasury bank account identity does not match configuration");
        }
        String masked = "****" + parts[1].substring(Math.max(0, parts[1].length() - 4));
        return new TreasuryAccountReference(
                state.financialInstitutionCode(),
                configured.configurationId(),
                reference, // Existing T0 mapping requires canonical age-ncp-clc, not a hash.
                masked,
                configured.version()
        );
    }

    private static Map<String, TreasuryResolutionProperties.Account> findInstitution(
            Map<String, Map<String, TreasuryResolutionProperties.Account>> accounts,
            String institution
    ) {
        var matches = accounts.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(institution)).toList();
        if (matches.size() != 1 || matches.getFirst().getValue() == null) {
            throw new IllegalStateException("Treasury institution configuration is absent or ambiguous");
        }
        return matches.getFirst().getValue();
    }

    private static TreasuryResolutionProperties.Account findClaim(
            Map<String, TreasuryResolutionProperties.Account> accounts,
            String claimType
    ) {
        var matches = accounts.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(claimType)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Treasury claim configuration is absent or ambiguous");
        }
        return matches.getFirst().getValue();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
