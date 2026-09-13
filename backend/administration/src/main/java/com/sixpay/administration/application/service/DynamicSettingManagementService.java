package com.sixpay.administration.application.service;

import com.sixpay.administration.application.port.input.DynamicSettingManagementUseCase;
import com.sixpay.administration.application.port.input.SettingRegistryQueryUseCase;
import com.sixpay.administration.application.port.output.DynamicSettingEventPublisher;
import com.sixpay.administration.application.port.output.DynamicSettingHistoryRepositoryPort;
import com.sixpay.administration.application.port.output.DynamicSettingRepositoryPort;
import com.sixpay.administration.domain.exception.DynamicSettingNotFoundException;
import com.sixpay.administration.domain.exception.DynamicSettingVersionNotFoundException;
import com.sixpay.administration.domain.model.*;
import com.sixpay.common.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DynamicSettingManagementService implements DynamicSettingManagementUseCase {

    private final SettingRegistryQueryUseCase registry;
    private final DynamicSettingRepositoryPort repository;
    private final DynamicSettingHistoryRepositoryPort historyRepository;
    private final DynamicSettingEventPublisher eventPublisher;
    private final DynamicSettingValueValidator validator = new DynamicSettingValueValidator();
    private final TimeProvider timeProvider;

    public DynamicSettingManagementService(
            SettingRegistryQueryUseCase registry,
            DynamicSettingRepositoryPort repository,
            DynamicSettingHistoryRepositoryPort historyRepository,
            DynamicSettingEventPublisher eventPublisher,
            TimeProvider timeProvider
    ) {
        this.registry = registry;
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
    }

    @Override
    public List<SettingDefinition> definitions() {
        return registry.definitions().stream()
                .sorted(Comparator.comparing(SettingDefinition::key))
                .toList();
    }

    @Override
    public DynamicSettingValue get(String key) {
        SettingDefinition definition = requireDefinition(key);
        return repository.findByKey(definition.key())
                .orElseGet(() -> defaultValue(definition));
    }

    @Override
    @Transactional
    public DynamicSettingValue update(String key, String value, String reason, String actor) {
        SettingDefinition definition = requireDefinition(key);
        validator.validate(definition, value);
        requireReason(reason);
        requireActor(actor);

        DynamicSettingValue current = repository.findByKey(definition.key())
                .orElseGet(() -> defaultValue(definition));

        if (current.value().equals(value.strip())) {
            return current;
        }

        Instant now = timeProvider.now();
        DynamicSettingValue saved = repository.save(new DynamicSettingValue(
                definition.key(),
                definition.domain(),
                value.strip(),
                current.version() + 1,
                now,
                actor.strip(),
                reason.strip()
        ));

        historyRepository.append(new DynamicSettingHistoryEntry(
                UUID.randomUUID(),
                definition.key(),
                definition.domain(),
                current.value(),
                saved.value(),
                current.version(),
                saved.version(),
                now,
                actor.strip(),
                reason.strip(),
                "UPDATE"
        ));

        eventPublisher.publishAfterCommit(new DynamicSettingChanged(
                saved.key(), saved.domain(), saved.value(), saved.version(), saved.updatedAt()
        ));
        return saved;
    }

    @Override
    public List<DynamicSettingHistoryEntry> history(String key) {
        SettingDefinition definition = requireDefinition(key);
        return historyRepository.findByKeyOrderByNewVersionDesc(definition.key());
    }

    @Override
    @Transactional
    public DynamicSettingValue rollback(String key, long targetVersion, String reason, String actor) {
        SettingDefinition definition = requireDefinition(key);
        requireReason(reason);
        requireActor(actor);

        DynamicSettingValue current = repository.findByKey(definition.key())
                .orElseGet(() -> defaultValue(definition));

        String targetValue;
        if (targetVersion == 1L) {
            targetValue = definition.defaultValue();
        } else {
            targetValue = historyRepository.findByKeyAndNewVersion(definition.key(), targetVersion)
                    .map(DynamicSettingHistoryEntry::newValue)
                    .orElseThrow(() -> new DynamicSettingVersionNotFoundException(definition.key(), targetVersion));
        }

        validator.validate(definition, targetValue);
        if (current.value().equals(targetValue)) {
            return current;
        }

        Instant now = timeProvider.now();
        DynamicSettingValue saved = repository.save(new DynamicSettingValue(
                definition.key(),
                definition.domain(),
                targetValue,
                current.version() + 1,
                now,
                actor.strip(),
                reason.strip()
        ));

        historyRepository.append(new DynamicSettingHistoryEntry(
                UUID.randomUUID(),
                definition.key(),
                definition.domain(),
                current.value(),
                saved.value(),
                current.version(),
                saved.version(),
                now,
                actor.strip(),
                reason.strip(),
                "ROLLBACK_TO_" + targetVersion
        ));

        eventPublisher.publishAfterCommit(new DynamicSettingChanged(
                saved.key(), saved.domain(), saved.value(), saved.version(), saved.updatedAt()
        ));
        return saved;
    }

    private SettingDefinition requireDefinition(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }
        return registry.find(key.strip())
                .orElseThrow(() -> new DynamicSettingNotFoundException(key.strip()));
    }

    private DynamicSettingValue defaultValue(SettingDefinition definition) {
        return new DynamicSettingValue(
                definition.key(),
                definition.domain(),
                definition.defaultValue(),
                1L,
                Instant.EPOCH,
                "SYSTEM_DEFAULT",
                "Catalog default"
        );
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason is required");
    }

    private static void requireActor(String actor) {
        if (actor == null || actor.isBlank()) throw new IllegalArgumentException("actor is required");
    }
}
