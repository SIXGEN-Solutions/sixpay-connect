package com.sixpay.notification.infrastructure.operational.persistence;

import com.sixpay.notification.configuration.NotificationApplicationAutoConfiguration;
import com.sixpay.notification.configuration.NotificationEmailAutoConfiguration;
import com.sixpay.notification.configuration.NotificationMessagingAutoConfiguration;
import com.sixpay.notification.configuration.NotificationPersistenceAutoConfiguration;
import com.sixpay.notification.configuration.NotificationRetryAutoConfiguration;
import com.sixpay.notification.configuration.NotificationRetryPolicyAutoConfiguration;
import com.sixpay.notification.configuration.OperationalNotificationApplicationAutoConfiguration;
import com.sixpay.notification.configuration.OperationalNotificationEmailAutoConfiguration;
import com.sixpay.notification.configuration.OperationalNotificationOperationsAutoConfiguration;
import com.sixpay.notification.configuration.OperationalNotificationPersistenceAutoConfiguration;
import com.sixpay.notification.configuration.OperationalNotificationRetryAutoConfiguration;
import com.sixpay.notification.domain.model.NotificationAttempt;
import com.sixpay.notification.domain.model.NotificationAttemptOutcome;
import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeduplicationKey;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationIntent;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationRecipientType;
import com.sixpay.notification.domain.model.NotificationReplayAudit;
import com.sixpay.notification.domain.model.NotificationSourceReference;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;
import com.sixpay.notification.domain.model.OperationalNotificationTriggerType;
import com.sixpay.notification.domain.repository.NotificationAttemptRepository;
import com.sixpay.notification.domain.repository.NotificationReplayRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationOperationsRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;
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

import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OperationalNotificationPersistenceIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class OperationalNotificationPersistenceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            );

    private static final Instant CREATED_AT =
            Instant.parse("2026-08-09T18:00:00Z");

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
    }

    @Autowired
    private OperationalNotificationRepository repository;

    @Autowired
    private NotificationAttemptRepository attemptRepository;

    @Autowired
    private OperationalNotificationOperationsRepository operationsRepository;

    @Autowired
    private NotificationReplayRepository replayRepository;

    @Autowired
    private OperationalNotificationReplaySpringDataRepository replayJpaRepository;

    @Autowired
    private OperationalNotificationAttemptSpringDataRepository attemptJpaRepository;

    @Autowired
    private OperationalNotificationSpringDataRepository notificationJpaRepository;

    @BeforeEach
    void cleanDatabase() {
        replayJpaRepository.deleteAllInBatch();
        attemptJpaRepository.deleteAllInBatch();
        notificationJpaRepository.deleteAllInBatch();
    }

    @Test
    void enforcesFunctionalIdempotenceAndReloadsTemplateVariables() {
        OperationalNotificationDelivery first =
                pending(
                        "11111111-1111-4111-8111-111111111111",
                        "a".repeat(64),
                        CREATED_AT
                );

        OperationalNotificationDelivery duplicate =
                pending(
                        "22222222-2222-4222-8222-222222222222",
                        "a".repeat(64),
                        CREATED_AT.plusSeconds(1)
                );

        var firstResult = repository.saveIfAbsent(first);
        var duplicateResult = repository.saveIfAbsent(duplicate);

        assertThat(firstResult.created()).isTrue();
        assertThat(duplicateResult.created()).isFalse();

        assertThat(
                duplicateResult.delivery()
                        .intent()
                        .notificationId()
        ).isEqualTo(
                first.intent().notificationId()
        );

        var reloaded = repository.findById(
                first.intent().notificationId()
        ).orElseThrow();

        assertThat(reloaded.intent().templateVariables())
                .containsEntry(
                        "paymentReference",
                        "PAY-20260809-0001"
                )
                .containsEntry(
                        "currency",
                        "XAF"
                );

        assertThat(
                repository.findByDeduplicationKey(
                        first.intent().deduplicationKey()
                )
        ).contains(reloaded);

        assertThat(
                operationsRepository.countByStatus(
                        NotificationDeliveryStatus.PENDING
                )
        ).isEqualTo(1);
    }

    @Test
    void ordersDueNotificationsAndClaimsEachCandidateOnlyOnce() {
        OperationalNotificationDelivery later =
                pending(
                        "33333333-3333-4333-8333-333333333333",
                        "b".repeat(64),
                        CREATED_AT.plusSeconds(60)
                );

        OperationalNotificationDelivery earlier =
                pending(
                        "44444444-4444-4444-8444-444444444444",
                        "c".repeat(64),
                        CREATED_AT
                );

        repository.saveIfAbsent(later);
        repository.saveIfAbsent(earlier);

        List<UUID> dueIds =
                repository.findDueNotificationIds(
                        CREATED_AT.plusSeconds(60),
                        10
                );

        assertThat(dueIds).containsExactly(
                earlier.intent().notificationId(),
                later.intent().notificationId()
        );

        var claimed = repository.claimForDispatch(
                earlier.intent().notificationId(),
                CREATED_AT.plusSeconds(60)
        ).orElseThrow();

        assertThat(claimed.intent().status())
                .isEqualTo(
                        NotificationDeliveryStatus.DISPATCHING
                );
        assertThat(claimed.attemptCount()).isEqualTo(1);
        assertThat(claimed.cycleAttemptCount()).isEqualTo(1);

        assertThat(
                repository.claimForDispatch(
                        earlier.intent().notificationId(),
                        CREATED_AT.plusSeconds(61)
                )
        ).isEmpty();
    }

    @Test
    void persistsAttemptsAndReturnsThemInAttemptOrder() {
        OperationalNotificationDelivery delivery =
                pending(
                        "55555555-5555-4555-8555-555555555555",
                        "d".repeat(64),
                        CREATED_AT
                );

        repository.saveIfAbsent(delivery);

        UUID notificationId =
                delivery.intent().notificationId();

        NotificationAttempt second =
                new NotificationAttempt(
                        UUID.fromString(
                                "66666666-6666-4666-8666-666666666666"
                        ),
                        notificationId,
                        2,
                        CREATED_AT.plusSeconds(30),
                        CREATED_AT.plusSeconds(31),
                        NotificationAttemptOutcome.FAILED_RETRYABLE,
                        "SMTP_SEND_FAILED"
                );

        NotificationAttempt first =
                new NotificationAttempt(
                        UUID.fromString(
                                "77777777-7777-4777-8777-777777777777"
                        ),
                        notificationId,
                        1,
                        CREATED_AT.plusSeconds(10),
                        CREATED_AT.plusSeconds(11),
                        NotificationAttemptOutcome.ACCEPTED,
                        null
                );

        attemptRepository.append(second);
        attemptRepository.append(first);

        assertThat(
                attemptRepository.findByNotificationId(
                        notificationId
                )
        )
                .extracting(
                        NotificationAttempt::attemptNumber
                )
                .containsExactly(1, 2);
    }

    @Test
    void replayDeadLetterPreservesNotificationIdentityAndCreatesAudit() {
        OperationalNotificationDelivery pending =
                pending(
                        "88888888-8888-4888-8888-888888888888",
                        "e".repeat(64),
                        CREATED_AT
                );

        repository.saveIfAbsent(pending);

        OperationalNotificationDelivery claimed =
                repository.claimForDispatch(
                        pending.intent().notificationId(),
                        CREATED_AT.plusSeconds(1)
                ).orElseThrow();

        OperationalNotificationDelivery dead =
                claimed.deadLetter(
                        "SMTP_SEND_FAILED"
                );

        repository.save(dead);

        Instant replayedAt =
                CREATED_AT.plusSeconds(120);

        NotificationReplayAudit audit =
                new NotificationReplayAudit(
                        UUID.fromString(
                                "99999999-9999-4999-8999-999999999999"
                        ),
                        dead.intent().notificationId(),
                        "ops-user-42",
                        "SMTP configuration corrected",
                        NotificationDeliveryStatus.DEAD_LETTERED,
                        replayedAt
                );

        OperationalNotificationDelivery replayed =
                replayRepository.replayDeadLetter(audit)
                        .orElseThrow();

        assertThat(replayed.intent().notificationId())
                .isEqualTo(
                        dead.intent().notificationId()
                );
        assertThat(
                replayed.intent().deduplicationKey()
        ).isEqualTo(
                dead.intent().deduplicationKey()
        );
        assertThat(replayed.intent().status())
                .isEqualTo(
                        NotificationDeliveryStatus.FAILED_RETRYABLE
                );
        assertThat(replayed.replayCount()).isEqualTo(1);
        assertThat(replayed.cycleAttemptCount()).isZero();
        assertThat(replayed.nextAttemptAt())
                .isEqualTo(replayedAt);

        assertThat(
                replayRepository.findReplaysByNotificationId(
                        dead.intent().notificationId()
                )
        )
                .singleElement()
                .satisfies(savedAudit -> {
                    assertThat(
                            savedAudit.operatorReference()
                    ).isEqualTo("ops-user-42");
                    assertThat(
                            savedAudit.previousStatus()
                    ).isEqualTo(
                            NotificationDeliveryStatus.DEAD_LETTERED
                    );
                });
    }

    @Test
    void exposesOperationalCountsAndOldestDueTimestamp() {
        repository.saveIfAbsent(
                pending(
                        "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                        "f".repeat(64),
                        CREATED_AT
                )
        );

        repository.saveIfAbsent(
                pending(
                        "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                        "1".repeat(64),
                        CREATED_AT.plusSeconds(30)
                )
        );

        Instant dueAt =
                CREATED_AT.plusSeconds(30);

        assertThat(
                operationsRepository.countDue(dueAt)
        ).isEqualTo(2);

        assertThat(
                operationsRepository.findOldestDueAt(dueAt)
        ).contains(CREATED_AT);

        assertThat(
                operationsRepository.findIdsByStatus(
                        NotificationDeliveryStatus.PENDING,
                        1
                )
        ).hasSize(1);
    }

    private static OperationalNotificationDelivery pending(
            String notificationId,
            String deduplicationKey,
            Instant createdAt
    ) {
        return OperationalNotificationDelivery.pending(
                new NotificationIntent(
                        UUID.fromString(notificationId),
                        new NotificationSourceReference(
                                OperationalNotificationTriggerType
                                        .PAYMENT_POSTED,
                                "payment-20260809-0001"
                        ),
                        new NotificationRecipient(
                                NotificationRecipientType
                                        .SIXPAY_ADMIN,
                                "operations-admin",
                                Locale.FRENCH
                        ),
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey
                                .PAYMENT_POSTED_ADMIN_V1,
                        new NotificationDeduplicationKey(
                                deduplicationKey
                        ),
                        Map.of(
                                "paymentId",
                                "payment-20260809-0001",
                                "paymentReference",
                                "PAY-20260809-0001",
                                "partnerId",
                                "TRESORPAY",
                                "amount",
                                "10000",
                                "currency",
                                "XAF",
                                "postedAt",
                                "2026-08-09T17:55:00Z"
                        ),
                        NotificationDeliveryStatus.PENDING,
                        createdAt,
                        "corr-operational-persistence"
                )
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            NotificationApplicationAutoConfiguration.class,
            NotificationEmailAutoConfiguration.class,
            NotificationMessagingAutoConfiguration.class,
            NotificationPersistenceAutoConfiguration.class,
            NotificationRetryPolicyAutoConfiguration.class,
            NotificationRetryAutoConfiguration.class,

            OperationalNotificationApplicationAutoConfiguration.class,
            OperationalNotificationEmailAutoConfiguration.class,
            OperationalNotificationOperationsAutoConfiguration.class,
            OperationalNotificationRetryAutoConfiguration.class
    })
    @ImportAutoConfiguration(
            OperationalNotificationPersistenceAutoConfiguration.class
    )
    static class TestApplication {

        @Bean
        ObjectMapper operationalNotificationTestObjectMapper() {
            return new ObjectMapper();
        }
    }
}
