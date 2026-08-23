package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.configuration.AdministrationModuleConfiguration;
import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;
import com.sixpay.administration.domain.repository.IncidentSearchCriteria;
import com.sixpay.administration.domain.repository.OperationalIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes =
                OperationalIncidentPersistenceIT
                        .TestApplication.class,
        webEnvironment =
                SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class OperationalIncidentPersistenceIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "sixpay_incidents"
                    )
                    .withUsername("sixpay")
                    .withPassword("sixpay-test");

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

        registry.add(
                "spring.flyway.locations",
                () ->
                        "filesystem:"
                                + "../bootstrap/"
                                + "src/main/resources/"
                                + "db/migration"
        );
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OperationalIncidentRepository repository;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update(
                "DELETE FROM operational_incident_timeline"
        );
        jdbc.update(
                "DELETE FROM operational_incident"
        );
    }

    @Test
    void loadsIncidentAndTimelineFromPostgreSql() {
        insertIncident(
                "INC-POSTGRES-001",
                "HIGH",
                "Accounting",
                "OPEN"
        );

        jdbc.update(
                "INSERT INTO operational_incident_timeline "
                        + "(event_id, incident_id, occurred_at, message, actor, sequence_no) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                "EVT-001",
                "INC-POSTGRES-001",
                Timestamp.from(
                        Instant.parse(
                                "2026-08-23T14:01:00Z"
                        )
                ),
                "Incident detected",
                "SYSTEM",
                0
        );

        var incident =
                repository.findById(
                                new IncidentId(
                                        "INC-POSTGRES-001"
                                )
                        )
                        .orElseThrow();

        assertThat(incident.component())
                .isEqualTo("Accounting");

        assertThat(incident.timeline())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.eventId())
                            .isEqualTo("EVT-001");
                    assertThat(entry.sequenceNo())
                            .isZero();
                });
    }

    @Test
    void searchesUsingDynamicPostgreSqlPredicates() {
        insertIncident(
                "INC-POSTGRES-002",
                "HIGH",
                "Accounting",
                "OPEN"
        );

        insertIncident(
                "INC-POSTGRES-003",
                "LOW",
                "Notification",
                "CLOSED"
        );

        var page =
                repository.search(
                        new IncidentSearchCriteria(
                                IncidentSeverity.HIGH,
                                IncidentStatus.OPEN,
                                "account",
                                0,
                                20
                        )
                );

        assertThat(page.totalElements())
                .isEqualTo(1);

        assertThat(page.content())
                .singleElement()
                .satisfies(
                        incident ->
                                assertThat(
                                        incident
                                                .incidentId()
                                                .value()
                                ).isEqualTo(
                                        "INC-POSTGRES-002"
                                )
                );
    }

    @Test
    void searchesWithNoOptionalFilter() {
        insertIncident(
                "INC-POSTGRES-004",
                "MEDIUM",
                "CoreBanking",
                "INVESTIGATING"
        );

        var page =
                repository.search(
                        new IncidentSearchCriteria(
                                null,
                                null,
                                null,
                                0,
                                20
                        )
                );

        assertThat(page.totalElements())
                .isEqualTo(1);
    }

    private void insertIncident(
            String id,
            String severity,
            String component,
            String status
    ) {
        Instant openedAt =
                Instant.parse(
                        "2026-08-23T14:00:00Z"
                );

        jdbc.update(
                "INSERT INTO operational_incident "
                        + "(incident_id, severity, component, summary, status, "
                        + "description, impact, opened_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                severity,
                component,
                "Synthetic test incident",
                status,
                "Persistence integration test fixture",
                "Test-only impact",
                Timestamp.from(openedAt),
                Timestamp.from(openedAt)
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(
            AdministrationModuleConfiguration.class
    )
    static class TestApplication {
    }
}
