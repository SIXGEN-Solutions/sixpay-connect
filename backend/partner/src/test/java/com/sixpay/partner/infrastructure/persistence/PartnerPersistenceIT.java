package com.sixpay.partner.infrastructure.persistence;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.partner.application.command.CreatePartnerCommand;
import com.sixpay.partner.application.port.input.PartnerManagementUseCase;
import com.sixpay.partner.configuration.PartnerModuleConfiguration;
import com.sixpay.partner.domain.model.PartnerStatus;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.partner.infrastructure.audit.PartnerAuditSpringDataRepository;
import com.sixpay.partner.infrastructure.idempotency.PartnerIdempotencySpringDataRepository;
import com.sixpay.partner.infrastructure.outbox.OutboxEventSpringDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PartnerPersistenceIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class PartnerPersistenceIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"));

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PartnerManagementUseCase management;

    @Autowired
    private PartnerSpringDataRepository repository;

    @Autowired
    private PartnerAuditSpringDataRepository auditRepository;

    @Autowired
    private PartnerIdempotencySpringDataRepository idempotencyRepository;

    @Autowired
    private OutboxEventSpringDataRepository outboxRepository;

    @Test
    void persistsPartnerAuditAndOutboxInPostgreSql() {
        var created = management.create(new CreatePartnerCommand(
                "Acme Payments",
                "Alice Ops",
                "alice.ops@example.com",
                Set.of("PAYMENT"),
                "admin@sixpay",
                CorrelationId.of("corr-it-001"),
                "idem-it-001"
        ));

        var entity = repository.findAggregateById(created.id()).orElseThrow();

        assertThat(entity.status()).isEqualTo(PartnerStatus.PENDING_VALIDATION);
        assertThat(entity.authorizedTransactionTypes()).containsExactly("PAYMENT");
        assertThat(auditRepository.count()).isOne();
        assertThat(idempotencyRepository.count()).isOne();
        assertThat(outboxRepository.count()).isOne();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(PartnerModuleConfiguration.class)
    static class TestApplication {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return () -> Optional.<AuthenticatedUser>empty();
        }
    }
}
