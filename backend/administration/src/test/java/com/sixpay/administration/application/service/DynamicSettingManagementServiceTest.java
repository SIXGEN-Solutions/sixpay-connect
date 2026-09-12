package com.sixpay.administration.application.service;

import com.sixpay.administration.application.port.output.*;
import com.sixpay.administration.domain.model.*;
import com.sixpay.common.time.TimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicSettingManagementServiceTest {

    @Test
    void updatePersistsHistoryAndPublishesEvent() {
        var values = new ValueRepo();
        var history = new HistoryRepo();
        var event = new AtomicReference<DynamicSettingChanged>();
        var service = service(values, history, event);

        DynamicSettingValue updated = service.update(
                "payment.callback.max-attempts", "10", "Operational tuning", "admin"
        );

        assertThat(updated.value()).isEqualTo("10");
        assertThat(updated.version()).isEqualTo(2);
        assertThat(history.entries).hasSize(1);
        assertThat(history.entries.getFirst().operation()).isEqualTo("UPDATE");
        assertThat(event.get()).isNotNull();
    }

    @Test
    void rollbackCreatesNewVersionAndPreservesHistory() {
        var values = new ValueRepo();
        var history = new HistoryRepo();
        var event = new AtomicReference<DynamicSettingChanged>();
        var service = service(values, history, event);

        service.update("payment.callback.max-attempts", "10", "Change 1", "admin");
        service.update("payment.callback.max-attempts", "12", "Change 2", "admin");

        DynamicSettingValue result = service.rollback(
                "payment.callback.max-attempts", 2, "Rollback", "admin"
        );

        assertThat(result.value()).isEqualTo("10");
        assertThat(result.version()).isEqualTo(4);
        assertThat(history.entries).hasSize(3);
        assertThat(history.entries.getLast().operation()).isEqualTo("ROLLBACK_TO_2");
    }

    private DynamicSettingManagementService service(
            ValueRepo values,
            HistoryRepo history,
            AtomicReference<DynamicSettingChanged> event
    ) {
        TimeProvider clock = () -> Instant.parse("2026-09-12T17:00:00Z");
        return new DynamicSettingManagementService(
                new DefaultSettingRegistry(), values, history, event::set, clock
        );
    }

    private static final class ValueRepo implements DynamicSettingRepositoryPort {
        private final Map<String, DynamicSettingValue> values = new HashMap<>();
        public Optional<DynamicSettingValue> findByKey(String key) { return Optional.ofNullable(values.get(key)); }
        public DynamicSettingValue save(DynamicSettingValue value) { values.put(value.key(), value); return value; }
    }

    private static final class HistoryRepo implements DynamicSettingHistoryRepositoryPort {
        private final List<DynamicSettingHistoryEntry> entries = new ArrayList<>();
        public DynamicSettingHistoryEntry append(DynamicSettingHistoryEntry e) { entries.add(e); return e; }
        public List<DynamicSettingHistoryEntry> findByKeyOrderByNewVersionDesc(String key) {
            return entries.stream().filter(e -> e.key().equals(key))
                    .sorted(Comparator.comparingLong(DynamicSettingHistoryEntry::newVersion).reversed()).toList();
        }
        public Optional<DynamicSettingHistoryEntry> findByKeyAndNewVersion(String key, long version) {
            return entries.stream().filter(e -> e.key().equals(key) && e.newVersion() == version).findFirst();
        }
    }
}
