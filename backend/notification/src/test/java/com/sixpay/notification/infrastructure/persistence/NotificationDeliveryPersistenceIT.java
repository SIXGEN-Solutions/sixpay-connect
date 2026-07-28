package com.sixpay.notification.infrastructure.persistence;

import com.sixpay.notification.application.model.NotificationDeliveryRegistration;
import com.sixpay.notification.application.port.out.NotificationDeliveryStore;
import com.sixpay.notification.configuration.NotificationPersistenceAutoConfiguration;
import com.sixpay.notification.configuration.NotificationApplicationAutoConfiguration;
import com.sixpay.notification.configuration.NotificationEmailAutoConfiguration;
import com.sixpay.notification.configuration.NotificationMessagingAutoConfiguration;
import com.sixpay.notification.configuration.NotificationRetryAutoConfiguration;
import com.sixpay.notification.configuration.NotificationRetryPolicyAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = NotificationDeliveryPersistenceIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class NotificationDeliveryPersistenceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private NotificationDeliveryStore store;

    @Autowired
    private NotificationDeliverySpringDataRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAllInBatch();
    }

    @Test
    void enforcesIdempotenceAndTracksSuccessfulDelivery() {
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-27T12:00:00Z");
        var registration = registration(eventId, createdAt);

        assertThat(store.tryStart(registration)).isTrue();
        assertThat(store.tryStart(registration)).isFalse();
        assertThat(repository.count()).isOne();

        var processing = repository.findByEventId(eventId).orElseThrow();
        assertThat(processing.status())
                .isEqualTo(NotificationDeliveryStatus.PROCESSING);
        assertThat(processing.attemptCount()).isOne();

        Instant sentAt = createdAt.plusSeconds(2);
        store.markSent(eventId, sentAt);

        var sent = repository.findByEventId(eventId).orElseThrow();
        assertThat(sent.status())
                .isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(sent.sentAt()).isEqualTo(sentAt);
        assertThat(sent.lastError()).isNull();
        assertThat(sent.nextAttemptAt()).isNull();
    }

    @Test
    void tracksFailedDeliveryForTheFutureRetryStep() {
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-27T13:00:00Z");
        store.tryStart(registration(eventId, createdAt));

        Instant retryAt = createdAt.plusSeconds(60);
        store.markFailed(
                eventId,
                "SMTP unavailable",
                createdAt.plusSeconds(1),
                retryAt
        );

        var failed = repository.findByEventId(eventId).orElseThrow();
        assertThat(failed.status())
                .isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(failed.lastError()).isEqualTo("SMTP unavailable");
        assertThat(failed.nextAttemptAt()).isEqualTo(retryAt);
        assertThat(failed.sentAt()).isNull();
    }

    @Test
    void claimsOnlyDueFailedDeliveryAndIncrementsAttemptCount() {
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-28T13:00:00Z");
        store.tryStart(registration(eventId, createdAt));
        Instant retryAt = createdAt.plusSeconds(60);
        store.markFailed(
                eventId,
                "SMTP unavailable",
                createdAt.plusSeconds(1),
                retryAt
        );

        assertThat(store.claimDue(retryAt.minusMillis(1), 10)).isEmpty();

        var claimed = store.claimDue(retryAt, 10);
        assertThat(claimed).singleElement().satisfies(attempt ->
                assertThat(attempt.attemptCount()).isEqualTo(2)
        );

        var processing = repository.findByEventId(eventId).orElseThrow();
        assertThat(processing.status())
                .isEqualTo(NotificationDeliveryStatus.PROCESSING);
        assertThat(processing.attemptCount()).isEqualTo(2);
    }

    @Test
    void neverClaimsSentDelivery() {
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-28T14:00:00Z");
        store.tryStart(registration(eventId, createdAt));
        store.markSent(eventId, createdAt.plusSeconds(1));

        assertThat(store.claimDue(createdAt.plusSeconds(60), 10))
                .isEmpty();
    }

    @Test
    void neverClaimsDeadDelivery() {
        UUID eventId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-07-28T15:00:00Z");
        store.tryStart(registration(eventId, createdAt));
        store.markDead(
                eventId,
                "Maximum attempts reached",
                createdAt.plusSeconds(1)
        );

        var dead = repository.findByEventId(eventId).orElseThrow();
        assertThat(dead.status()).isEqualTo(NotificationDeliveryStatus.DEAD);
        assertThat(dead.nextAttemptAt()).isNull();
        assertThat(store.claimDue(createdAt.plusSeconds(60), 10))
                .isEmpty();
    }

    private static NotificationDeliveryRegistration registration(
            UUID eventId,
            Instant createdAt
    ) {
        return new NotificationDeliveryRegistration(
                eventId,
                UUID.randomUUID(),
                "PartnerStatusChangedIntegrationEvent",
                "alice.ops@example.com",
                "partner-activated",
                null,
                "corr-persistence",
                createdAt
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            NotificationApplicationAutoConfiguration.class,
            NotificationEmailAutoConfiguration.class,
            NotificationMessagingAutoConfiguration.class,
            NotificationRetryPolicyAutoConfiguration.class,
            NotificationRetryAutoConfiguration.class
    })
    @ImportAutoConfiguration(
            NotificationPersistenceAutoConfiguration.class
    )
    static class TestApplication {
    }
}
