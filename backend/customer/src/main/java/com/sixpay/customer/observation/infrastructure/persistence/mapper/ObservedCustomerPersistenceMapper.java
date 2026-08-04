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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
        target.setObservedCustomerId(source.id().value());
        target.setNiuProtected(
                protector.protect(
                        source.identity().normalizedNiu()
                )
        );
        target.setNiuSearchHash(
                protector.searchHash(
                        source.identity().normalizedNiu()
                )
        );
        target.setLegalNameProtected(
                protector.protect(
                        source.identity().legalName()
                )
        );
        target.setLegalNameSearchNormalized(
                normalizeSearchName(
                        source.identity().legalName()
                )
        );
        target.setPhoneMasked(
                source.identity().phoneMasked()
        );
        target.setEmailMasked(
                source.identity().emailMasked()
        );
        target.setFirstObservedAt(
                source.firstObservedAt()
        );
        target.setLastObservedAt(
                source.lastObservedAt()
        );
        target.setTotalPayments(
                source.totalPayments()
        );
        target.setSuccessfulPayments(
                source.successfulPayments()
        );
        target.setFailedPayments(
                source.failedPayments()
        );
        target.setLastPaymentStatus(
                source.lastPaymentStatus().name()
        );
        target.setLastFailureReasonCode(
                source.lastFailureReasonCode()
                        .orElse(null)
        );
        target.setProjectionVersion(
                source.projectionVersion()
        );
        target.setSourceEventWatermark(
                source.sourceEventWatermark().value()
        );
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(
                    source.firstObservedAt()
            );
        }
        target.setUpdatedAt(source.updatedAt());
        target.replaceInstitutions(
                source.institutions().stream()
                        .map(this::toInstitutionEntity)
                        .toList()
        );
    }

    public ObservedCustomer toDomain(
            ObservedCustomerJpaEntity customer,
            List<ObservedPaymentJpaEntity> payments,
            List<ProcessedObservationEventJpaEntity> events
    ) {
        return ObservedCustomer.reconstitute(
                ObservedCustomerId.of(
                        customer.getObservedCustomerId()
                ),
                ObservedCustomerIdentity.of(
                        protector.reveal(
                                customer.getNiuProtected()
                        ),
                        protector.reveal(
                                customer.getLegalNameProtected()
                        ),
                        customer.getPhoneMasked(),
                        customer.getEmailMasked()
                ),
                customer.getInstitutions().stream()
                        .map(this::toInstitutionDomain)
                        .toList(),
                payments.stream()
                        .map(this::toPaymentDomain)
                        .toList(),
                events.stream()
                        .map(
                                ProcessedObservationEventJpaEntity
                                        ::getSourceEventId
                        )
                        .collect(Collectors.toUnmodifiableSet()),
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

    public ObservedPaymentJpaEntity toPaymentEntity(
            ObservedPaymentReference source,
            ObservedCustomerJpaEntity customer
    ) {
        ObservedPaymentJpaEntity target =
                new ObservedPaymentJpaEntity();
        copyPayment(source, customer, target);
        return target;
    }

    public void copyPayment(
            ObservedPaymentReference source,
            ObservedCustomerJpaEntity customer,
            ObservedPaymentJpaEntity target
    ) {
        target.setPaymentId(source.paymentId());
        target.setObservedCustomer(customer);
        target.setPublicPaymentReference(
                source.paymentReference()
        );
        target.setFinancialInstitutionCode(
                source.financialInstitutionCode()
        );
        target.setAmount(source.amount());
        target.setCurrency(source.currency());
        target.setPaymentStatus(
                source.status().name()
        );
        target.setFailureReasonCode(
                source.failureReasonCode()
        );
        target.setPaymentCreatedAt(
                source.createdAt()
        );
        target.setPaymentUpdatedAt(
                source.updatedAt()
        );
    }

    public ProcessedObservationEventJpaEntity toEventEntity(
            UUID sourceEventId,
            ObservedCustomerJpaEntity customer,
            ProjectionWatermark watermark,
            java.time.Instant observedAt
    ) {
        ProcessedObservationEventJpaEntity target =
                new ProcessedObservationEventJpaEntity();
        target.setSourceEventId(sourceEventId);
        target.setObservedCustomer(customer);
        target.setSourceEventWatermark(
                watermark.value()
        );
        target.setObservedAt(observedAt);
        target.setProcessedAt(observedAt);
        return target;
    }

    private ObservedCustomerInstitutionJpaEntity
            toInstitutionEntity(
                    ObservedCustomerInstitution source
            ) {
        ObservedCustomerInstitutionJpaEntity target =
                new ObservedCustomerInstitutionJpaEntity();
        target.setFinancialInstitutionCode(
                source.financialInstitutionCode()
        );
        target.setFirstObservedAt(
                source.firstObservedAt()
        );
        target.setLastObservedAt(
                source.lastObservedAt()
        );
        target.replaceAccounts(
                source.accounts().stream()
                        .map(this::toAccountEntity)
                        .toList()
        );
        return target;
    }

    private ObservedAccountJpaEntity toAccountEntity(
            ObservedAccountReference source
    ) {
        ObservedAccountJpaEntity target =
                new ObservedAccountJpaEntity();
        target.setAccountBindingFingerprint(
                source.accountBindingFingerprint()
        );
        target.setMaskedValue(source.maskedValue());
        return target;
    }

    private ObservedCustomerInstitution toInstitutionDomain(
            ObservedCustomerInstitutionJpaEntity source
    ) {
        return ObservedCustomerInstitution.of(
                source.getFinancialInstitutionCode(),
                source.getFirstObservedAt(),
                source.getLastObservedAt(),
                source.getAccounts().stream()
                        .map(account ->
                                ObservedAccountReference.of(
                                        account
                                                .getAccountBindingFingerprint(),
                                        account.getMaskedValue()
                                )
                        )
                        .toList()
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

    private static String normalizeSearchName(
            String value
    ) {
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
