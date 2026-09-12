package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.application.port.output.DynamicSettingHistoryRepositoryPort;
import com.sixpay.administration.domain.model.DynamicSettingHistoryEntry;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DynamicSettingHistoryRepositoryAdapter implements DynamicSettingHistoryRepositoryPort {

    private final DynamicSettingHistorySpringDataRepository repository;

    public DynamicSettingHistoryRepositoryAdapter(DynamicSettingHistorySpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public DynamicSettingHistoryEntry append(DynamicSettingHistoryEntry entry) {
        return toDomain(repository.save(new DynamicSettingHistoryJpaEntity(
                entry.historyId(), entry.key(), entry.domain(),
                entry.previousValue(), entry.newValue(),
                entry.previousVersion(), entry.newVersion(),
                entry.changedAt(), entry.changedBy(),
                entry.reason(), entry.operation()
        )));
    }

    @Override
    public List<DynamicSettingHistoryEntry> findByKeyOrderByNewVersionDesc(String key) {
        return repository.findByKeyOrderByNewVersionDesc(key).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<DynamicSettingHistoryEntry> findByKeyAndNewVersion(String key, long version) {
        return repository.findByKeyAndNewVersion(key, version).map(this::toDomain);
    }

    private DynamicSettingHistoryEntry toDomain(DynamicSettingHistoryJpaEntity entity) {
        return new DynamicSettingHistoryEntry(
                entity.getHistoryId(), entity.getKey(), entity.getDomain(),
                entity.getPreviousValue(), entity.getNewValue(),
                entity.getPreviousVersion(), entity.getNewVersion(),
                entity.getChangedAt(), entity.getChangedBy(),
                entity.getReason(), entity.getOperation()
        );
    }
}
