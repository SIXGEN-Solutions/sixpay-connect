package com.sixpay.tests.e2e;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.integration.messaging.outbox.OutboxRelay;
import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.port.in.HandleIntegrationEventUseCase;
import com.sixpay.notification.application.port.out.PartnerNotificationSender;
import com.sixpay.notification.infrastructure.persistence.NotificationDeliverySpringDataRepository;
import com.sixpay.notification.infrastructure.persistence.NotificationDeliveryStatus;
import com.sixpay.partner.application.command.CreatePartnerCommand;
import com.sixpay.partner.application.command.DecidePartnerCommand;
import com.sixpay.partner.application.command.PartnerDecision;
import com.sixpay.partner.application.port.in.PartnerManagementUseCase;
import com.sixpay.partner.domain.model.PartnerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.sixpay.security.authentication.CurrentUserProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = GoldenModuleE2EIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("e2e")
@Testcontainers
@Import(GoldenModuleE2EIT.RecordingSenderConfiguration.class)
class GoldenModuleE2EIT {

    private static final String CORRELATION_ID = "corr-golden-e2e";

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
    private PartnerManagementUseCase partnerManagement;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private HandleIntegrationEventUseCase notificationHandler;

    @Autowired
    private NotificationDeliverySpringDataRepository deliveries;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RecordingPartnerNotificationSender sender;

    @BeforeEach
    void resetSender() {
        sender.clear();
    }

    @Test
    void partnerApprovalIsDeliveredExactlyOnceAcrossTheGoldenFlow() {
        var partner = partnerManagement.create(new CreatePartnerCommand(
                "Golden Partner",
                "Alice Operations",
                "alice.ops@example.com",
                Set.of("PAYMENT"),
                "golden-e2e",
                new CorrelationId(CORRELATION_ID),
                "golden-create-" + UUID.randomUUID()
        ));

        var approved = partnerManagement.decide(new DecidePartnerCommand(
                new PartnerId(partner.id()),
                PartnerDecision.APPROVE,
                null,
                "golden-e2e",
                new CorrelationId(CORRELATION_ID),
                "golden-approve-" + UUID.randomUUID()
        ));

        assertThat(approved.status().name()).isEqualTo("ACTIVE");

        outboxRelay.poll();

        UUID eventId = jdbc.queryForObject(
                """
                SELECT event_id
                  FROM sixpay.notification_deliveries
                 WHERE aggregate_id = ?
                   AND template = 'partner-activated'
                """,
                UUID.class,
                partner.id()
        );

        var delivery = deliveries.findByEventId(eventId).orElseThrow();
        assertThat(delivery.status())
                .isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(delivery.attemptCount()).isOne();
        assertThat(delivery.recipient())
                .isEqualTo("alice.ops@example.com");
        assertThat(delivery.template()).isEqualTo("partner-activated");
        assertThat(delivery.correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(delivery.sentAt()).isNotNull();
        assertThat(delivery.lastError()).isNull();
        assertThat(delivery.nextAttemptAt()).isNull();

        assertThat(sender.notifications())
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.eventId()).isEqualTo(eventId);
                    assertThat(notification.partnerId()).isEqualTo(partner.id());
                    assertThat(notification.decision())
                            .isEqualTo(
                                    PartnerDecisionNotification.Decision.APPROVED
                            );
                });

        assertThat(jdbc.queryForObject(
                """
                SELECT status
                  FROM sixpay.partner_outbox_events
                 WHERE event_id = ?
                """,
                String.class,
                eventId
        )).isEqualTo("PUBLISHED");

        String payload = jdbc.queryForObject(
                """
                SELECT payload::text
                  FROM sixpay.partner_outbox_events
                 WHERE event_id = ?
                """,
                String.class,
                eventId
        );

        var duplicate = new IntegrationEventEnvelope(
                eventId,
                "PartnerStatusChangedIntegrationEvent",
                2,
                "PARTNER",
                partner.id(),
                CORRELATION_ID,
                Instant.now(),
                payload
        );

        notificationHandler.handle(duplicate);
        notificationHandler.handle(duplicate);

        assertThat(sender.notifications()).hasSize(1);
        assertThat(deliveries.findByEventId(eventId).orElseThrow().status())
                .isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(deliveries.findByEventId(eventId).orElseThrow().attemptCount())
                .isOne();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingSenderConfiguration {

        @Bean
        @Primary
        RecordingPartnerNotificationSender recordingPartnerNotificationSender() {
            return new RecordingPartnerNotificationSender();
        }

        @Bean
        CurrentUserProvider currentUserProvider() {
            return java.util.Optional::empty;
        }
    }

    static final class RecordingPartnerNotificationSender
            implements PartnerNotificationSender {

        private final CopyOnWriteArrayList<PartnerDecisionNotification>
                notifications = new CopyOnWriteArrayList<>();

        @Override
        public void send(PartnerDecisionNotification notification) {
            notifications.add(notification);
        }

        void clear() {
            notifications.clear();
        }

        java.util.List<PartnerDecisionNotification> notifications() {
            return java.util.List.copyOf(notifications);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
