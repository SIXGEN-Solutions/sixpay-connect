package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.application.port.output.DynamicSettingRepositoryPort;
import com.sixpay.administration.domain.model.DynamicSettingValue;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DynamicSettingRepositoryAdapter implements DynamicSettingRepositoryPort {

    private final DynamicSettingSpringDataRepository repository;

    public DynamicSettingRepositoryAdapter(DynamicSettingSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<DynamicSettingValue> findByKey(String key) {
        return repository.findById(key).map(this::toDomain);
    }

    @Override
    public DynamicSettingValue save(DynamicSettingValue value) {
        DynamicSettingJpaEntity entity = repository.findById(value.key()).orElse(null);
        if (entity == null) {
            entity = new DynamicSettingJpaEntity(
                    value.key(), value.domain(), value.value(),
                    value.updatedAt(), value.updatedBy(), value.reason()
            );
        } else {
            entity.apply(
                    value.domain(), value.value(),
                    value.updatedAt(), value.updatedBy(), value.reason()
            );
        }
        return toDomain(repository.save(entity));
    }

    private DynamicSettingValue toDomain(DynamicSettingJpaEntity entity) {
        return new DynamicSettingValue(
                entity.getKey(), entity.getDomain(), entity.getValue(),
                entity.getVersion(), entity.getUpdatedAt(),
                entity.getUpdatedBy(), entity.getReason()
        );
    }
}
