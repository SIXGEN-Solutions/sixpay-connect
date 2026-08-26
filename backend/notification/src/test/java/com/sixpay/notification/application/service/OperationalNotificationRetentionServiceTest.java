package com.sixpay.notification.application.service;

import com.sixpay.notification.application.port.output.OperationalNotificationOperationsTelemetry;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.repository.OperationalNotificationOperationsRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalNotificationRetentionServiceTest {

    @Test
    void purgeUsesSeparateDeliveredAndFailureRetention() {
        Instant now =
                Instant.parse("2026-08-07T17:00:00Z");

        CapturingRepository repository =
                new CapturingRepository();

        AtomicInteger purgedMetric =
                new AtomicInteger();

        var service =
                new OperationalNotificationRetentionService(
                        repository,
                        Duration.ofDays(90),
                        Duration.ofDays(365),
                        500,
                        new OperationalNotificationOperationsTelemetry() {
                            @Override
                            public void recordReplay() {
                            }

                            @Override
                            public void recordPurged(int count) {
                                purgedMetric.addAndGet(count);
                            }
                        }
                );

        var report = service.purge(now);

        assertEquals(7, report.deleted());
        assertEquals(
                now.minus(Duration.ofDays(90)),
                repository.deliveredBefore
        );
        assertEquals(
                now.minus(Duration.ofDays(365)),
                repository.failedBefore
        );
        assertEquals(500, repository.limit);
        assertEquals(7, purgedMetric.get());
    }

    private static final class CapturingRepository
            implements OperationalNotificationOperationsRepository {

        private Instant deliveredBefore;
        private Instant failedBefore;
        private int limit;

        @Override
        public List<UUID> findIdsByStatus(
                NotificationDeliveryStatus status,
                int limit
        ) {
            return List.of();
        }

        @Override
        public long countByStatus(
                NotificationDeliveryStatus status
        ) {
            return 0;
        }

        @Override
        public long countDue(
                Instant dueAt
        ) {
            return 0;
        }

        @Override
        public Optional<Instant> findOldestDueAt(
                Instant dueAt
        ) {
            return Optional.empty();
        }

        @Override
        public int purgeTerminal(
                Instant deliveredBefore,
                Instant failedBefore,
                int limit
        ) {
            this.deliveredBefore = deliveredBefore;
            this.failedBefore = failedBefore;
            this.limit = limit;
            return 7;
        }
    }
}
