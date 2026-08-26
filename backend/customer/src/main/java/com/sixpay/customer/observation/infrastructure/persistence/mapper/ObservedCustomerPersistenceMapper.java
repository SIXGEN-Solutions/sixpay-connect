package com.sixpay.customer.observation.infrastructure.persistence.mapper;

import com.sixpay.customer.observation.domain.model.ObservedAccountReference;
import com.sixpay.customer.observation.domain.model.ObservedCustomer;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedCustomerIdentity;
import com.sixpay.customer.observation.domain.model.ObservedCustomerInstitution;
import com.sixpay.customer.observation.domain.model.ObservedPaymentReference;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import com.sixpay.customer.observation.domain.model.ProjectionWatermark;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedAccountJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedCustomerInstitutionJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedCustomerJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedPaymentJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ProcessedObservationEventJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.protection.ObservedCustomerDataProtector;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ObservedCustomerPersistenceMapper {

    private final ObservedCustomerDataProtector protector;

    public ObservedCustomerPersistenceMapper(
            ObservedCustomerDataProtector protector
    ) {
        this.protector = Objects.requireNonNull(
                protector,
                "protector is required"
        );
    }

    public void copyToEntity(
            ObservedCustomer source,
            ObservedCustomerJpaEntity target
    ) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(target, "target is required");

        target.setObservedCustomerId(source.id().value());
        target.setNiuProtected(
                protector.protect(source.identity().normalizedNiu())
        );
        target.setNiuSearchHash(
                protector.searchHash(source.identity().normalizedNiu())
        );
        target.setLegalNameProtected(
                protector.protect(source.identity().legalName())
        );
        target.setLegalNameSearchNormalized(
                normalizeSearchName(source.identity().legalName())
        );
        target.setPhoneMasked(source.identity().phoneMasked());
        target.setEmailMasked(source.identity().emailMasked());
        target.setFirstObservedAt(source.firstObservedAt());
        target.setLastObservedAt(source.lastObservedAt());
        target.setTotalPayments(source.totalPayments());
        target.setSuccessfulPayments(source.successfulPayments());
        target.setFailedPayments(source.failedPayments());
        target.setLastPaymentStatus(source.lastPaymentStatus().name());
        target.setLastFailureReasonCode(
                source.lastFailureReasonCode().orElse(null)
        );
        target.setProjectionVersion(source.projectionVersion());
        target.setSourceEventWatermark(
                source.sourceEventWatermark().value()
        );

        if (target.getCreatedAt() == null) {
            target.setCreatedAt(source.firstObservedAt());
        }

        target.setUpdatedAt(source.updatedAt());

        synchronizeInstitutions(source, target);
    }

    public ObservedCustomer toDomain(
            ObservedCustomerJpaEntity customer,
            List<ObservedPaymentJpaEntity> payments,
            List<ProcessedObservationEventJpaEntity> events
    ) {
        Objects.requireNonNull(customer, "customer is required");
        Objects.requireNonNull(payments, "payments is required");
        Objects.requireNonNull(events, "events is required");

        List<ObservedCustomerInstitution> institutions =
                customer.getInstitutions()
                        .stream()
                        .map(this::toInstitutionDomain)
                        .toList();

        List<ObservedPaymentReference> paymentReferences =
                payments.stream()
                        .map(this::toPaymentDomain)
                        .toList();

        Set<UUID> appliedSourceEventIds =
                events.stream()
                        .map(ProcessedObservationEventJpaEntity::getSourceEventId)
                        .collect(Collectors.toUnmodifiableSet());

        return ObservedCustomer.reconstitute(
                ObservedCustomerId.of(customer.getObservedCustomerId()),
                ObservedCustomerIdentity.of(
                        protector.reveal(customer.getNiuProtected()),
                        protector.reveal(customer.getLegalNameProtected()),
                        customer.getPhoneMasked(),
                        customer.getEmailMasked()
                ),
                institutions,
                paymentReferences,
                appliedSourceEventIds,
                customer.getFirstObservedAt(),
                customer.getLastObservedAt(),
                customer.getTotalPayments(),
                customer.getSuccessfulPayments(),
                customer.getFailedPayments(),
                ObservedPaymentStatus.valueOf(
                        customer.getLastPaymentStatus()
                ),
                customer.getLastFailureReasonCode(),
                customer.getProjectionVersion(),
                ProjectionWatermark.of(
                        customer.getSourceEventWatermark()
                ),
                customer.getUpdatedAt()
        );
    }

    public void copyPayment(
            ObservedPaymentReference source,
            ObservedCustomerJpaEntity customer,
            ObservedPaymentJpaEntity target
    ) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(customer, "customer is required");
        Objects.requireNonNull(target, "target is required");

        target.setPaymentId(source.paymentId());
        target.setObservedCustomer(customer);
        target.setPublicPaymentReference(source.paymentReference());
        target.setFinancialInstitutionCode(
                source.financialInstitutionCode()
        );
        target.setAmount(source.amount());
        target.setCurrency(source.currency());
        target.setPaymentStatus(source.status().name());
        target.setFailureReasonCode(source.failureReasonCode());
        target.setPaymentCreatedAt(source.createdAt());
        target.setPaymentUpdatedAt(source.updatedAt());
    }

    public ProcessedObservationEventJpaEntity toEventEntity(
            UUID sourceEventId,
            ObservedCustomerJpaEntity customer,
            ProjectionWatermark watermark,
            Instant observedAt
    ) {
        Objects.requireNonNull(
                sourceEventId,
                "sourceEventId is required"
        );
        Objects.requireNonNull(
                customer,
                "customer is required"
        );
        Objects.requireNonNull(
                watermark,
                "watermark is required"
        );
        Objects.requireNonNull(
                observedAt,
                "observedAt is required"
        );

        ProcessedObservationEventJpaEntity target =
                ProcessedObservationEventJpaEntity.create();

        target.setSourceEventId(sourceEventId);
        target.setObservedCustomer(customer);
        target.setSourceEventWatermark(watermark.value());
        target.setObservedAt(observedAt);
        target.setProcessedAt(observedAt);
        return target;
    }

    private void synchronizeInstitutions(
            ObservedCustomer source,
            ObservedCustomerJpaEntity target
    ) {
        Map<String, ObservedCustomerInstitutionJpaEntity>
                existingByCode =
                target.mutableInstitutions()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        ObservedCustomerInstitutionJpaEntity
                                                ::getFinancialInstitutionCode,
                                        Function.identity(),
                                        (left, right) -> left,
                                        HashMap::new
                                )
                        );

        Set<String> expectedCodes = new HashSet<>();

        for (ObservedCustomerInstitution domainInstitution
                : source.institutions()) {

            String code =
                    domainInstitution.financialInstitutionCode();

            expectedCodes.add(code);

            ObservedCustomerInstitutionJpaEntity entity =
                    existingByCode.get(code);

            if (entity == null) {
                entity =
                        ObservedCustomerInstitutionJpaEntity.create();
                target.addInstitution(entity);
            }

            entity.setFinancialInstitutionCode(code);
            entity.setFirstObservedAt(
                    domainInstitution.firstObservedAt()
            );
            entity.setLastObservedAt(
                    domainInstitution.lastObservedAt()
            );

            synchronizeAccounts(domainInstitution, entity);
        }

        List<ObservedCustomerInstitutionJpaEntity> obsolete =
                target.mutableInstitutions()
                        .stream()
                        .filter(entity ->
                                !expectedCodes.contains(
                                        entity.getFinancialInstitutionCode()
                                )
                        )
                        .toList();

        obsolete.forEach(target::removeInstitution);
    }

    private void synchronizeAccounts(
            ObservedCustomerInstitution source,
            ObservedCustomerInstitutionJpaEntity target
    ) {
        Map<String, ObservedAccountJpaEntity>
                existingByFingerprint =
                target.mutableAccounts()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        ObservedAccountJpaEntity
                                                ::getAccountBindingFingerprint,
                                        Function.identity(),
                                        (left, right) -> left,
                                        HashMap::new
                                )
                        );

        Set<String> expectedFingerprints = new HashSet<>();

        for (ObservedAccountReference domainAccount
                : source.accounts()) {

            String fingerprint =
                    domainAccount.accountBindingFingerprint();

            expectedFingerprints.add(fingerprint);

            ObservedAccountJpaEntity entity =
                    existingByFingerprint.get(fingerprint);

            if (entity == null) {
                entity = ObservedAccountJpaEntity.create();
                target.addAccount(entity);
            }

            entity.setAccountBindingFingerprint(fingerprint);
            entity.setMaskedValue(domainAccount.maskedValue());
        }

        List<ObservedAccountJpaEntity> obsolete =
                target.mutableAccounts()
                        .stream()
                        .filter(entity ->
                                !expectedFingerprints.contains(
                                        entity
                                                .getAccountBindingFingerprint()
                                )
                        )
                        .toList();

        obsolete.forEach(target::removeAccount);
    }

    private ObservedCustomerInstitution toInstitutionDomain(
            ObservedCustomerInstitutionJpaEntity source
    ) {
        return ObservedCustomerInstitution.of(
                source.getFinancialInstitutionCode(),
                source.getFirstObservedAt(),
                source.getLastObservedAt(),
                source.getAccounts()
                        .stream()
                        .map(this::toAccountDomain)
                        .toList()
        );
    }

    private ObservedAccountReference toAccountDomain(
            ObservedAccountJpaEntity source
    ) {
        return ObservedAccountReference.of(
                source.getAccountBindingFingerprint(),
                source.getMaskedValue()
        );
    }

    private ObservedPaymentReference toPaymentDomain(
            ObservedPaymentJpaEntity source
    ) {
        return new ObservedPaymentReference(
                source.getPaymentId(),
                source.getPublicPaymentReference(),
                source.getFinancialInstitutionCode(),
                source.getAmount(),
                source.getCurrency(),
                ObservedPaymentStatus.valueOf(
                        source.getPaymentStatus()
                ),
                source.getFailureReasonCode(),
                source.getPaymentCreatedAt(),
                source.getPaymentUpdatedAt()
        );
    }

    private static String normalizeSearchName(String value) {
        Objects.requireNonNull(value, "legal name is required");

        String decomposed = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        return decomposed
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{Alnum}]+", " ")
                .strip()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }
}
