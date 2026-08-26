package com.sixpay.partner.domain.model;

import com.sixpay.partner.domain.event.PartnerCreated;
import com.sixpay.partner.domain.event.PartnerDomainEvent;
import com.sixpay.partner.domain.event.PartnerStatusChanged;
import com.sixpay.partner.domain.event.PartnerThresholdConfigured;
import com.sixpay.partner.domain.exception.PartnerDomainException;
import com.sixpay.sharedkernel.domain.model.AggregateRoot;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Partner extends AggregateRoot<PartnerId> {

    private static final int MAX_REASON_LENGTH = 500;

    private final PartnerName legalName;
    private final TechnicalContact technicalContact;
    private final AuthorizedPerimeter authorizedPerimeter;
    private final Instant createdAt;
    private final Map<String, ValidationThreshold> validationThresholds;

    private PartnerStatus status;
    private String statusReason;
    private Instant updatedAt;

    private Partner(
            PartnerId id,
            PartnerName legalName,
            TechnicalContact technicalContact,
            AuthorizedPerimeter authorizedPerimeter,
            PartnerStatus status,
            String statusReason,
            Instant createdAt,
            Instant updatedAt,
            Collection<ValidationThreshold> thresholds
    ) {
        super(id);
        this.legalName = Objects.requireNonNull(legalName, "legalName is required");
        this.technicalContact = Objects.requireNonNull(technicalContact, "technicalContact is required");
        this.authorizedPerimeter = Objects.requireNonNull(authorizedPerimeter, "authorizedPerimeter is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.statusReason = normalizeOptionalReason(statusReason);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.validationThresholds = new LinkedHashMap<>();
        thresholds.forEach(threshold -> this.validationThresholds.put(thresholdKey(threshold), threshold));
    }

    public static Partner create(
            PartnerId id,
            PartnerName legalName,
            TechnicalContact technicalContact,
            AuthorizedPerimeter authorizedPerimeter,
            Instant now
    ) {
        var partner = new Partner(
                id,
                legalName,
                technicalContact,
                authorizedPerimeter,
                PartnerStatus.PENDING_VALIDATION,
                null,
                now,
                now,
                List.of()
        );
        partner.registerDomainEvent(new PartnerCreated(id, now));
        return partner;
    }

    public static Partner reconstitute(
            PartnerId id,
            PartnerName legalName,
            TechnicalContact technicalContact,
            AuthorizedPerimeter authorizedPerimeter,
            PartnerStatus status,
            String statusReason,
            Instant createdAt,
            Instant updatedAt,
            Collection<ValidationThreshold> thresholds
    ) {
        return new Partner(
                id,
                legalName,
                technicalContact,
                authorizedPerimeter,
                status,
                statusReason,
                createdAt,
                updatedAt,
                thresholds
        );
    }

    public void approve(Instant now) {
        requireStatus(PartnerStatus.PENDING_VALIDATION, "approve");
        transitionTo(PartnerStatus.ACTIVE, null, now);
    }

    public void reject(String reason, Instant now) {
        requireStatus(PartnerStatus.PENDING_VALIDATION, "reject");
        transitionTo(PartnerStatus.REJECTED, requireReason(reason), now);
    }

    public void suspend(String reason, Instant now) {
        requireStatus(PartnerStatus.ACTIVE, "suspend");
        transitionTo(PartnerStatus.SUSPENDED, requireReason(reason), now);
    }

    public void reactivate(Instant now) {
        requireStatus(PartnerStatus.SUSPENDED, "reactivate");
        transitionTo(PartnerStatus.ACTIVE, null, now);
    }

    public void configureValidationThreshold(ValidationThreshold threshold, Instant now) {
        Objects.requireNonNull(threshold, "threshold is required");
        Objects.requireNonNull(now, "now is required");
        if (status == PartnerStatus.REJECTED) {
            throw new PartnerDomainException("validation thresholds cannot be configured for a rejected partner");
        }
        if (!authorizedPerimeter.allows(threshold.transactionType())) {
            throw new PartnerDomainException(
                    "transaction type is outside the partner authorized perimeter: " + threshold.transactionType());
        }
        validationThresholds.put(thresholdKey(threshold), threshold);
        updatedAt = now;
        registerDomainEvent(new PartnerThresholdConfigured(id(), threshold, now));
    }

    public Optional<ValidationThreshold> thresholdFor(String transactionType, String currency) {
        return Optional.ofNullable(validationThresholds.get(
                AuthorizedPerimeter.normalize(transactionType) + ":" + normalizeCurrency(currency)));
    }

    public int requiredValidationLevels(
            String transactionType,
            String currency,
            BigDecimal transactionAmount
    ) {
        Objects.requireNonNull(
                transactionAmount,
                "transactionAmount is required"
        );
        if (transactionAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "transactionAmount must be positive"
            );
        }
        return thresholdFor(transactionType, currency)
                .filter(threshold ->
                        transactionAmount.compareTo(threshold.amount()) > 0)
                .map(ValidationThreshold::validationLevels)
                .orElse(1);
    }

    public boolean acceptsNewTransactions() {
        return status == PartnerStatus.ACTIVE;
    }

    public List<PartnerDomainEvent> pullDomainEvents() {
        return releaseDomainEvents().stream()
                .map(PartnerDomainEvent.class::cast)
                .toList();
    }

    private void requireStatus(PartnerStatus expected, String operation) {
        if (status != expected) {
            throw new PartnerDomainException(
                    "cannot " + operation + " partner in status " + status + "; expected " + expected);
        }
    }

    private void transitionTo(PartnerStatus target, String reason, Instant now) {
        Objects.requireNonNull(now, "now is required");
        var previous = status;
        status = target;
        statusReason = normalizeOptionalReason(reason);
        updatedAt = now;
        registerDomainEvent(new PartnerStatusChanged(
                id(),
                previous,
                target,
                statusReason,
                now
        ));
    }

    private static String requireReason(String reason) {
        var normalized = normalizeOptionalReason(reason);
        if (normalized == null) {
            throw new PartnerDomainException("a reason is required");
        }
        return normalized;
    }

    private static String normalizeOptionalReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        var normalized = reason.strip();
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new PartnerDomainException("reason must not exceed " + MAX_REASON_LENGTH + " characters");
        }
        return normalized;
    }

    private static String thresholdKey(ValidationThreshold threshold) {
        return threshold.transactionType() + ":" + threshold.currency();
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        return java.util.Currency.getInstance(
                currency.strip().toUpperCase(java.util.Locale.ROOT)).getCurrencyCode();
    }

    public PartnerName legalName() {
        return legalName;
    }

    public TechnicalContact technicalContact() {
        return technicalContact;
    }

    public AuthorizedPerimeter authorizedPerimeter() {
        return authorizedPerimeter;
    }

    public PartnerStatus status() {
        return status;
    }

    public Optional<String> statusReason() {
        return Optional.ofNullable(statusReason);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Collection<ValidationThreshold> validationThresholds() {
        return List.copyOf(validationThresholds.values());
    }
}
