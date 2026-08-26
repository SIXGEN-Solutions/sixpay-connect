package com.sixpay.partner.infrastructure.idempotency;

import com.sixpay.partner.application.port.output.PartnerIdempotencyStore;
import com.sixpay.partner.domain.model.PartnerId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class PartnerIdempotencyStoreAdapter implements PartnerIdempotencyStore {

    private final PartnerIdempotencySpringDataRepository repository;
    private final EntityManager entityManager;

    public PartnerIdempotencyStoreAdapter(
            PartnerIdempotencySpringDataRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public void lock(String operation, String idempotencyKey) {
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0))")
                .setParameter("lockKey", operation + ":" + idempotencyKey)
                .getSingleResult();
    }

    @Override
    public Optional<PartnerId> findCompleted(String operation, String idempotencyKey) {
        return repository.findByOperationAndIdempotencyKey(operation, idempotencyKey)
                .map(entity -> new PartnerId(entity.partnerId()));
    }

    @Override
    public void complete(
            String operation,
            String idempotencyKey,
            PartnerId partnerId,
            Instant completedAt
    ) {
        repository.save(new PartnerIdempotencyJpaEntity(
                operation,
                idempotencyKey,
                partnerId.value(),
                completedAt
        ));
    }
}
