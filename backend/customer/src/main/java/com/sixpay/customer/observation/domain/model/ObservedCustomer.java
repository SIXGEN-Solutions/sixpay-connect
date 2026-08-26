package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.customer.observation.domain.policy.ObservedCustomerIdentityPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Non-authoritative projection of a customer observed through Payment
 * activity.
 *
 * <p>All mutations are exposed through named business operations. The
 * projection never obtains the current time or generates identifiers.</p>
 */
public final class ObservedCustomer {

    private final ObservedCustomerId id;
    private ObservedCustomerIdentity identity;
    private final List<ObservedCustomerInstitution> institutions;
    private final Map<UUID, ObservedPaymentReference> payments;
    private final Set<UUID> appliedSourceEventIds;

    private Instant firstObservedAt;
    private Instant lastObservedAt;
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private ObservedPaymentStatus lastPaymentStatus;
    private String lastFailureReasonCode;
    private long projectionVersion;
    private ProjectionWatermark sourceEventWatermark;
    private Instant updatedAt;

    private ObservedCustomer(
            ObservedCustomerId id,
            ObservedCustomerIdentity identity,
            List<ObservedCustomerInstitution> institutions,
            List<ObservedPaymentReference> payments,
            Set<UUID> appliedSourceEventIds,
            Instant firstObservedAt,
            Instant lastObservedAt,
            long totalPayments,
            long successfulPayments,
            long failedPayments,
            ObservedPaymentStatus lastPaymentStatus,
            String lastFailureReasonCode,
            long projectionVersion,
            ProjectionWatermark sourceEventWatermark,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(
                id,
                "observedCustomerId is required"
        );
        this.identity = Objects.requireNonNull(
                identity,
                "identity is required"
        );
        this.institutions = new ArrayList<>(
                validateInstitutions(institutions)
        );
        this.payments = validatePayments(payments);
        this.appliedSourceEventIds = new HashSet<>(
                validateAppliedSourceEventIds(appliedSourceEventIds)
        );
        this.firstObservedAt = Objects.requireNonNull(
                firstObservedAt,
                "firstObservedAt is required"
        );
        this.lastObservedAt = Objects.requireNonNull(
                lastObservedAt,
                "lastObservedAt is required"
        );

        if (lastObservedAt.isBefore(firstObservedAt)) {
            throw new ObservedCustomerDomainException(
                    "lastObservedAt must not be before firstObservedAt"
            );
        }
        if (totalPayments != this.payments.size()) {
            throw new ObservedCustomerDomainException(
                    "totalPayments must equal the number of "
                            + "observed payments"
            );
        }

        validateCounters(
                totalPayments,
                successfulPayments,
                failedPayments
        );

        this.totalPayments = totalPayments;
        this.successfulPayments = successfulPayments;
        this.failedPayments = failedPayments;
        this.lastPaymentStatus = Objects.requireNonNull(
                lastPaymentStatus,
                "lastPaymentStatus is required"
        );
        this.lastFailureReasonCode = normalizeFailureReason(
                lastFailureReasonCode
        );

        if (projectionVersion < 1) {
            throw new ObservedCustomerDomainException(
                    "projectionVersion must be at least one"
            );
        }

        this.projectionVersion = projectionVersion;
        this.sourceEventWatermark = Objects.requireNonNull(
                sourceEventWatermark,
                "sourceEventWatermark is required"
        );
        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );

        if (updatedAt.isBefore(lastObservedAt)) {
            throw new ObservedCustomerDomainException(
                    "updatedAt must not be before lastObservedAt"
            );
        }

        validateInstitutionIntervals();
        validateDerivedCounters();
    }

    public static ObservedCustomer observeFirst(
            ObservedCustomerId id,
            ObservedCustomerObservation observation
    ) {
        Objects.requireNonNull(
                observation,
                "observation is required"
        );

        ObservedPaymentReference payment =
                observation.payment();

        return new ObservedCustomer(
                id,
                observation.identity(),
                List.of(observation.institution()),
                List.of(payment),
                Set.of(observation.sourceEventId()),
                observation.observedAt(),
                observation.observedAt(),
                1,
                payment.status().countsAsSuccessful() ? 1 : 0,
                payment.status().countsAsFailed() ? 1 : 0,
                payment.status(),
                payment.failureReasonCode(),
                1,
                observation.watermark(),
                observation.appliedAt()
        );
    }

    public ObservationApplicationResult observePayment(
            ObservedCustomerObservation observation
    ) {
        Objects.requireNonNull(
                observation,
                "observation is required"
        );

        if (appliedSourceEventIds.contains(
                observation.sourceEventId()
        )) {
            return ObservationApplicationResult.REPLAYED;
        }

        ObservedCustomerIdentityPolicy.requireCompatible(
                identity,
                observation.identity()
        );

        mergeIdentity(observation.identity());
        mergeInstitution(observation.institution());

        ObservedPaymentReference current =
                payments.get(observation.payment().paymentId());

        ObservationApplicationResult result;

        if (current == null) {
            applyNewPayment(observation.payment());
            result =
                    ObservationApplicationResult.APPLIED_NEW_PAYMENT;
        } else if (observation.payment().updatedAt()
                .isBefore(current.updatedAt())) {
            /*
             * The event is retained for idempotence and may already have
             * enriched institution/account history, but it cannot replace the
             * current payment status.
             */
            result =
                    ObservationApplicationResult.APPLIED_STALE_HISTORY;
        } else {
            applyPaymentUpdate(
                    current,
                    observation.payment()
            );
            result =
                    ObservationApplicationResult.APPLIED_PAYMENT_UPDATE;
        }

        appliedSourceEventIds.add(observation.sourceEventId());
        lastObservedAt = max(
                lastObservedAt,
                observation.observedAt()
        );
        updatedAt = max(
                updatedAt,
                observation.appliedAt()
        );
        sourceEventWatermark = observation.watermark();
        projectionVersion++;

        return result;
    }

    public static ObservedCustomer reconstitute(
            ObservedCustomerId id,
            ObservedCustomerIdentity identity,
            List<ObservedCustomerInstitution> institutions,
            List<ObservedPaymentReference> payments,
            Set<UUID> appliedSourceEventIds,
            Instant firstObservedAt,
            Instant lastObservedAt,
            long totalPayments,
            long successfulPayments,
            long failedPayments,
            ObservedPaymentStatus lastPaymentStatus,
            String lastFailureReasonCode,
            long projectionVersion,
            ProjectionWatermark sourceEventWatermark,
            Instant updatedAt
    ) {
        return new ObservedCustomer(
                id,
                identity,
                institutions,
                payments,
                appliedSourceEventIds,
                firstObservedAt,
                lastObservedAt,
                totalPayments,
                successfulPayments,
                failedPayments,
                lastPaymentStatus,
                lastFailureReasonCode,
                projectionVersion,
                sourceEventWatermark,
                updatedAt
        );
    }

    private void applyNewPayment(
            ObservedPaymentReference payment
    ) {
        payments.put(payment.paymentId(), payment);
        totalPayments++;
        incrementCategory(payment.status());
        updateLastPaymentConclusion(payment);
    }

    private void applyPaymentUpdate(
            ObservedPaymentReference current,
            ObservedPaymentReference candidate
    ) {
        decrementCategory(current.status());
        incrementCategory(candidate.status());
        payments.put(candidate.paymentId(), candidate);
        updateLastPaymentConclusion(candidate);
    }

    private void updateLastPaymentConclusion(
            ObservedPaymentReference payment
    ) {
        lastPaymentStatus = payment.status();
        lastFailureReasonCode =
                normalizeFailureReason(
                        payment.failureReasonCode()
                );
    }

    private void incrementCategory(
            ObservedPaymentStatus status
    ) {
        if (status.countsAsSuccessful()) {
            successfulPayments++;
        }
        if (status.countsAsFailed()) {
            failedPayments++;
        }
    }

    private void decrementCategory(
            ObservedPaymentStatus status
    ) {
        if (status.countsAsSuccessful()) {
            successfulPayments--;
        }
        if (status.countsAsFailed()) {
            failedPayments--;
        }
    }

    private void mergeIdentity(
            ObservedCustomerIdentity candidate
    ) {
        String phone = candidate.phoneMasked() != null
                ? candidate.phoneMasked()
                : identity.phoneMasked();
        String email = candidate.emailMasked() != null
                ? candidate.emailMasked()
                : identity.emailMasked();

        identity = ObservedCustomerIdentity.of(
                identity.normalizedNiu(),
                candidate.legalName(),
                phone,
                email
        );
    }

    private void mergeInstitution(
            ObservedCustomerInstitution candidate
    ) {
        for (int index = 0; index < institutions.size(); index++) {
            ObservedCustomerInstitution current =
                    institutions.get(index);

            if (current.financialInstitutionCode().equals(
                    candidate.financialInstitutionCode()
            )) {
                institutions.set(
                        index,
                        mergeInstitution(current, candidate)
                );
                return;
            }
        }

        institutions.add(candidate);
    }

    private static ObservedCustomerInstitution mergeInstitution(
            ObservedCustomerInstitution current,
            ObservedCustomerInstitution candidate
    ) {
        Map<String, ObservedAccountReference> accounts =
                new LinkedHashMap<>();

        current.accounts().forEach(account ->
                accounts.put(
                        account.accountBindingFingerprint(),
                        account
                )
        );
        candidate.accounts().forEach(account ->
                accounts.put(
                        account.accountBindingFingerprint(),
                        account
                )
        );

        return ObservedCustomerInstitution.of(
                current.financialInstitutionCode(),
                min(
                        current.firstObservedAt(),
                        candidate.firstObservedAt()
                ),
                max(
                        current.lastObservedAt(),
                        candidate.lastObservedAt()
                ),
                List.copyOf(accounts.values())
        );
    }

    private static List<ObservedCustomerInstitution>
            validateInstitutions(
                    List<ObservedCustomerInstitution> institutions
            ) {
        List<ObservedCustomerInstitution> copy = List.copyOf(
                Objects.requireNonNull(
                        institutions,
                        "institutions are required"
                )
        );

        if (copy.isEmpty()
                || copy.stream().anyMatch(Objects::isNull)) {
            throw new ObservedCustomerDomainException(
                    "Observed Customer must contain non-null institutions"
            );
        }

        Set<String> codes = new HashSet<>();
        for (ObservedCustomerInstitution institution : copy) {
            if (!codes.add(
                    institution.financialInstitutionCode()
            )) {
                throw new ObservedCustomerDomainException(
                        "financial institution codes must be unique"
                );
            }
        }

        return copy;
    }

    private static Map<UUID, ObservedPaymentReference>
            validatePayments(
                    List<ObservedPaymentReference> payments
            ) {
        List<ObservedPaymentReference> copy = List.copyOf(
                Objects.requireNonNull(
                        payments,
                        "payments are required"
                )
        );

        if (copy.isEmpty()
                || copy.stream().anyMatch(Objects::isNull)) {
            throw new ObservedCustomerDomainException(
                    "Observed Customer must contain non-null payments"
            );
        }

        Map<UUID, ObservedPaymentReference> indexed =
                new LinkedHashMap<>();

        for (ObservedPaymentReference payment : copy) {
            if (indexed.put(payment.paymentId(), payment) != null) {
                throw new ObservedCustomerDomainException(
                        "payment identifiers must be unique"
                );
            }
        }

        return indexed;
    }

    private static Set<UUID> validateAppliedSourceEventIds(
            Set<UUID> sourceEventIds
    ) {
        Set<UUID> copy = Set.copyOf(
                Objects.requireNonNull(
                        sourceEventIds,
                        "appliedSourceEventIds are required"
                )
        );

        if (copy.isEmpty()
                || copy.stream().anyMatch(Objects::isNull)) {
            throw new ObservedCustomerDomainException(
                    "appliedSourceEventIds must contain "
                            + "non-null identifiers"
            );
        }

        return copy;
    }

    private void validateInstitutionIntervals() {
        for (ObservedCustomerInstitution institution : institutions) {
            if (institution.firstObservedAt()
                    .isBefore(firstObservedAt)
                    || institution.lastObservedAt()
                    .isAfter(lastObservedAt)) {
                throw new ObservedCustomerDomainException(
                        "institution observation interval must fit "
                                + "inside customer observation interval"
                );
            }
        }
    }

    private void validateDerivedCounters() {
        long successful = payments.values().stream()
                .filter(payment ->
                        payment.status().countsAsSuccessful()
                )
                .count();
        long failed = payments.values().stream()
                .filter(payment ->
                        payment.status().countsAsFailed()
                )
                .count();

        if (successful != successfulPayments
                || failed != failedPayments) {
            throw new ObservedCustomerDomainException(
                    "payment counters must match observed payment statuses"
            );
        }
    }

    private static void validateCounters(
            long total,
            long successful,
            long failed
    ) {
        if (total < 1) {
            throw new ObservedCustomerDomainException(
                    "totalPayments must be at least one"
            );
        }
        if (successful < 0 || failed < 0) {
            throw new ObservedCustomerDomainException(
                    "payment counters must not be negative"
            );
        }
        if (successful + failed > total) {
            throw new ObservedCustomerDomainException(
                    "successfulPayments plus failedPayments "
                            + "must not exceed totalPayments"
            );
        }
    }

    private static String normalizeFailureReason(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value
                .strip()
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return null;
        }
        if (!normalized.matches(
                "^[A-Z0-9][A-Z0-9._-]{0,63}$"
        )) {
            throw new ObservedCustomerDomainException(
                    "lastFailureReasonCode has an invalid format"
            );
        }

        return normalized;
    }

    private static Instant min(
            Instant left,
            Instant right
    ) {
        return left.isBefore(right) ? left : right;
    }

    private static Instant max(
            Instant left,
            Instant right
    ) {
        return left.isAfter(right) ? left : right;
    }

    public ObservedCustomerId id() {
        return id;
    }

    public ObservedCustomerIdentity identity() {
        return identity;
    }

    public List<ObservedCustomerInstitution> institutions() {
        return List.copyOf(institutions);
    }

    public List<ObservedPaymentReference> payments() {
        return List.copyOf(payments.values());
    }

    public Set<UUID> appliedSourceEventIds() {
        return Set.copyOf(appliedSourceEventIds);
    }

    public Instant firstObservedAt() {
        return firstObservedAt;
    }

    public Instant lastObservedAt() {
        return lastObservedAt;
    }

    public long totalPayments() {
        return totalPayments;
    }

    public long successfulPayments() {
        return successfulPayments;
    }

    public long failedPayments() {
        return failedPayments;
    }

    public ObservedPaymentStatus lastPaymentStatus() {
        return lastPaymentStatus;
    }

    public Optional<String> lastFailureReasonCode() {
        return Optional.ofNullable(lastFailureReasonCode);
    }

    public long projectionVersion() {
        return projectionVersion;
    }

    public ProjectionWatermark sourceEventWatermark() {
        return sourceEventWatermark;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "ObservedCustomer[id="
                + id
                + ", identity=[PROTECTED]"
                + ", institutions="
                + institutions.size()
                + ", payments="
                + payments.size()
                + ", appliedSourceEvents="
                + appliedSourceEventIds.size()
                + ", firstObservedAt="
                + firstObservedAt
                + ", lastObservedAt="
                + lastObservedAt
                + ", totalPayments="
                + totalPayments
                + ", successfulPayments="
                + successfulPayments
                + ", failedPayments="
                + failedPayments
                + ", lastPaymentStatus="
                + lastPaymentStatus
                + ", lastFailureReasonCode="
                + lastFailureReasonCode
                + ", projectionVersion="
                + projectionVersion
                + ", sourceEventWatermark=[PROTECTED]"
                + ", updatedAt="
                + updatedAt
                + "]";
    }
}
