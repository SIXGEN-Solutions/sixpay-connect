package com.sixpay.partner.infrastructure.persistence;

import com.sixpay.partner.domain.model.AuthorizedPerimeter;
import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerName;
import com.sixpay.partner.domain.model.TechnicalContact;
import com.sixpay.partner.domain.model.ValidationThreshold;
import com.sixpay.partner.domain.repository.PartnerRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PartnerRepositoryAdapter implements PartnerRepository {

    private final PartnerSpringDataRepository repository;

    public PartnerRepositoryAdapter(PartnerSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Partner save(Partner partner) {
        var entity = repository.findAggregateById(partner.id().value())
                .orElseGet(() -> PartnerJpaEntity.create(partner));
        entity.synchronize(partner);
        repository.save(entity);
        return partner;
    }

    @Override
    public Optional<Partner> findById(PartnerId partnerId) {
        return repository.findAggregateById(partnerId.value()).map(this::toDomain);
    }

    @Override
    public boolean existsById(PartnerId partnerId) {
        return repository.existsById(partnerId.value());
    }

    private Partner toDomain(PartnerJpaEntity entity) {
        return Partner.reconstitute(
                new PartnerId(entity.id()),
                new PartnerName(entity.legalName()),
                new TechnicalContact(entity.technicalContactName(), entity.technicalContactEmail()),
                new AuthorizedPerimeter(entity.authorizedTransactionTypes()),
                entity.status(),
                entity.statusReason(),
                entity.createdAt(),
                entity.updatedAt(),
                entity.validationThresholds().stream()
                        .map(threshold -> new ValidationThreshold(
                                threshold.transactionType(),
                                threshold.currency(),
                                threshold.amount(),
                                threshold.validationLevels()))
                        .toList()
        );
    }
}
