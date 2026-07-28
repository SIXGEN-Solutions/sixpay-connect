package com.sixpay.partner.infrastructure.outbox;

import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.partner.configuration.PartnerModuleConfiguration;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PartnerOutboxConcurrencyIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false"
        }
)
@Testcontainers
class PartnerOutboxConcurrencyIT {

    private static final Instant NOW =
            Instant.parse("2026-07-27T10:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
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
    private OutboxEventSpringDataRepository repository;

    @Autowired
    private PartnerOutboxMessageSource messageSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearOutbox() {
        repository.deleteAll();
    }

    @Test
    void flywayCreatesTheIndustrializedOutboxColumns() {
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.columns
                 WHERE table_name = 'partner_outbox_events'
                   AND column_name IN (
                       'schema_version',
                       'correlation_id',
                       'attempt_count',
                       'next_attempt_at',
                       'last_attempt_at',
                       'claimed_at',
                       'claimed_by'
                   )
                """, Integer.class);

        assertThat(columnCount).isEqualTo(7);
    }

    @Test
    void concurrentClaimsExposeAnEventToOnlyOneInstance()
            throws Exception {
        UUID eventId = savePendingEvent();
        PartnerOutboxMessageSource firstInstance =
                new PartnerOutboxMessageSource(repository);
        PartnerOutboxMessageSource secondInstance =
                new PartnerOutboxMessageSource(repository);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<List<OutboxMessage>> first = executor.submit(
                    () -> claimAfterBarrier(firstInstance, start)
            );
            Future<List<OutboxMessage>> second = executor.submit(
                    () -> claimAfterBarrier(secondInstance, start)
            );

            List<OutboxMessage> firstClaim =
                    first.get(10, TimeUnit.SECONDS);
            List<OutboxMessage> secondClaim =
                    second.get(10, TimeUnit.SECONDS);

            assertThat(firstClaim.size() + secondClaim.size()).isOne();
            assertThat(
                    java.util.stream.Stream.concat(
                                    firstClaim.stream(),
                                    secondClaim.stream()
                            )
                            .map(message -> message.event().eventId())
            ).containsExactly(eventId);
            assertThat(statusOf(eventId)).isEqualTo("PROCESSING");
            assertThat(attemptCountOf(eventId)).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void retryWindowIsEnforcedBeforeTerminalDeadState() {
        UUID eventId = savePendingEvent();

        List<OutboxMessage> firstAttempt = messageSource.claimPending(
                10,
                NOW,
                Duration.ofMinutes(5)
        );
        messageSource.markFailed(
                eventId,
                "IllegalStateException",
                NOW,
                NOW.plusSeconds(30)
        );

        assertThat(firstAttempt).hasSize(1);
        assertThat(messageSource.claimPending(
                10,
                NOW.plusSeconds(29),
                Duration.ofMinutes(5)
        )).isEmpty();

        List<OutboxMessage> secondAttempt =
                messageSource.claimPending(
                        10,
                        NOW.plusSeconds(31),
                        Duration.ofMinutes(5)
                );
        messageSource.markDead(
                eventId,
                "IllegalStateException",
                NOW.plusSeconds(31)
        );

        assertThat(secondAttempt)
                .singleElement()
                .extracting(OutboxMessage::attemptCount)
                .isEqualTo(2);
        assertThat(statusOf(eventId)).isEqualTo("DEAD");
        assertThat(attemptCountOf(eventId)).isEqualTo(2);
    }

    @Test
    void interruptedClaimBecomesEligibleAfterProcessingTimeout() {
        UUID eventId = savePendingEvent();

        List<OutboxMessage> firstAttempt = messageSource.claimPending(
                10,
                NOW,
                Duration.ofMinutes(5)
        );

        assertThat(firstAttempt).hasSize(1);
        assertThat(messageSource.claimPending(
                10,
                NOW.plus(Duration.ofMinutes(4)),
                Duration.ofMinutes(5)
        )).isEmpty();

        List<OutboxMessage> recoveredAttempt =
                messageSource.claimPending(
                        10,
                        NOW.plus(Duration.ofMinutes(6)),
                        Duration.ofMinutes(5)
                );

        assertThat(recoveredAttempt)
                .singleElement()
                .extracting(OutboxMessage::attemptCount)
                .isEqualTo(2);
        assertThat(statusOf(eventId)).isEqualTo("PROCESSING");
    }

    private List<OutboxMessage> claimAfterBarrier(
            PartnerOutboxMessageSource source,
            CyclicBarrier start
    ) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        return new TransactionTemplate(transactionManager).execute(
                status -> source.claimPending(
                        10,
                        NOW,
                        Duration.ofMinutes(5)
                )
        );
    }

    private UUID savePendingEvent() {
        UUID eventId = UUID.randomUUID();
        repository.save(new OutboxEventJpaEntity(
                eventId,
                UUID.randomUUID(),
                "PartnerStatusChangedIntegrationEvent",
                1,
                "correlation-" + eventId,
                "{\"currentStatus\":\"ACTIVE\"}",
                NOW,
                NOW
        ));
        return eventId;
    }

    private String statusOf(UUID eventId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT status
                  FROM partner_outbox_events
                 WHERE event_id = ?
                """,
                String.class,
                eventId
        );
    }

    private Integer attemptCountOf(UUID eventId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT attempt_count
                  FROM partner_outbox_events
                 WHERE event_id = ?
                """,
                Integer.class,
                eventId
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(PartnerModuleConfiguration.class)
    static class TestApplication {

        @org.springframework.context.annotation.Bean
        CurrentUserProvider currentUserProvider() {
            return () -> Optional.<AuthenticatedUser>empty();
        }
    }
}
