package com.sixpay.bootstrap.integration.administration;

import com.sixpay.administration.api.AdministrationQueryController;
import com.sixpay.administration.api.IncidentQueryController;
import com.sixpay.administration.application.port.input.AdministrationQueryUseCase;
import com.sixpay.administration.application.port.input.IncidentQueryUseCase;
import com.sixpay.administration.application.port.output.IntegrationHealthQueryPort;
import com.sixpay.administration.application.service.AdministrationQueryService;
import com.sixpay.administration.application.service.IncidentQueryService;
import com.sixpay.administration.domain.repository.OperationalIncidentRepository;
import com.sixpay.administration.infrastructure.configuration.SpringConfigurationAdministrationSettingsAdapter;
import com.sixpay.administration.infrastructure.persistence.IncidentTimelineJpaEntity;
import com.sixpay.administration.infrastructure.persistence.OperationalIncidentJpaEntity;
import com.sixpay.administration.infrastructure.persistence.OperationalIncidentRepositoryAdapter;
import com.sixpay.administration.infrastructure.persistence.OperationalIncidentSpringDataRepository;
import com.sixpay.common.time.SystemTimeProvider;
import com.sixpay.common.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes =
                AdministrationIncidentFullStackConformanceTest
                        .TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "sixpay.accounting.batch.cutoff-zone=Africa/Douala",
                "sixpay.accounting.batch.cutoff-time=23:59"
        }
)
@ActiveProfiles("test")
@Testcontainers
class AdministrationIncidentFullStackConformanceTest {

    private static final String CORRELATION =
            "X-Correlation-Id";

    private static final String CORRELATION_ID =
            "11111111-1111-4111-8111-111111111111";

    private static final String INCIDENT_ID =
            "INC-FS-1-4-9";

    private static final String EVENT_ID =
            "EVT-FS-1-4-9-1";

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    "postgres:16-alpine"
            )
                    .withDatabaseName(
                            "sixpay_fs_1_4_9"
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
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders
                        .webAppContextSetup(
                                webApplicationContext
                        )
                        .build();

        seedRealIncident();
    }

    private void seedRealIncident() {
        jdbc.update(
                "DELETE FROM operational_incident_timeline"
        );

        jdbc.update(
                "DELETE FROM operational_incident"
        );

        Instant openedAt =
                Instant.parse(
                        "2026-08-23T15:00:00Z"
                );

        Instant updatedAt =
                openedAt.plusSeconds(300);

        jdbc.update(
                "INSERT INTO operational_incident "
                        + "(incident_id, severity, component, "
                        + "summary, status, description, impact, "
                        + "opened_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                INCIDENT_ID,
                "HIGH",
                "Accounting",
                "Accounting batch posting delayed",
                "OPEN",
                "Accounting posting acknowledgement "
                        + "was not observed.",
                "Accounting reconciliation requires review.",
                Timestamp.from(openedAt),
                Timestamp.from(updatedAt)
        );

        jdbc.update(
                "INSERT INTO operational_incident_timeline "
                        + "(event_id, incident_id, occurred_at, "
                        + "message, actor, sequence_no) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                EVENT_ID,
                INCIDENT_ID,
                Timestamp.from(
                        openedAt.plusSeconds(60)
                ),
                "Incident detected by accounting supervision",
                "SYSTEM",
                0
        );
    }

    @Test
    void realAdministrationUseCasesAndPersistenceAdaptersAreRegistered() {
        assertThat(
                context.getBeansOfType(
                        AdministrationQueryUseCase.class
                )
        ).isNotEmpty();

        assertThat(
                context.getBeansOfType(
                        IncidentQueryUseCase.class
                )
        ).isNotEmpty();

        assertThat(
                context.getBeansOfType(
                        OperationalIncidentRepository.class
                )
        ).isNotEmpty();

        assertThat(
                context.getBeansOfType(
                        OperationalIncidentSpringDataRepository.class
                )
        ).isNotEmpty();
    }

    @Test
    @WithMockUser(
            username = "fs-admin",
            roles = "ADMIN"
    )
    void incidentListExecutesThroughSpringAndRealPostgreSql()
            throws Exception {

        mvc.perform(
                        get(
                                "/internal/api/v1/incidents"
                        )
                                .param(
                                        "severity",
                                        "HIGH"
                                )
                                .param(
                                        "status",
                                        "OPEN"
                                )
                                .param(
                                        "component",
                                        "Accounting"
                                )
                                .param(
                                        "page",
                                        "0"
                                )
                                .param(
                                        "size",
                                        "20"
                                )
                                .header(
                                        CORRELATION,
                                        CORRELATION_ID
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].incidentId"
                        ).value(INCIDENT_ID)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].component"
                        ).value("Accounting")
                );
    }

    @Test
    @WithMockUser(
            username = "fs-auditor",
            roles = "AUDITOR"
    )
    void incidentDetailLoadsTimelineFromRealPostgreSql()
            throws Exception {

        mvc.perform(
                        get(
                                "/internal/api/v1/incidents/{incidentId}",
                                INCIDENT_ID
                        )
                                .header(
                                        CORRELATION,
                                        CORRELATION_ID
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.incidentId"
                        ).value(INCIDENT_ID)
                )
                .andExpect(
                        jsonPath(
                                "$.timeline[0].eventId"
                        ).value(EVENT_ID)
                )
                .andExpect(
                        jsonPath(
                                "$.timeline[0].actor"
                        ).value("SYSTEM")
                );
    }

    @Test
    @WithMockUser(
            username = "fs-admin",
            roles = "ADMIN"
    )
    void administrationOverviewWorksInAssembledContext()
            throws Exception {

        mvc.perform(
                        get(
                                "/internal/api/v1/administration/overview"
                        )
                                .header(
                                        CORRELATION,
                                        CORRELATION_ID
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.settings.accountingCutoffZone"
                        ).value("Africa/Douala")
                )
                .andExpect(
                        jsonPath(
                                "$.settings.accountingCutoffTime"
                        ).value("23:59")
                )
                .andExpect(
                        jsonPath(
                                "$.integrations"
                        ).isArray()
                )
                .andExpect(
                        jsonPath(
                                "$.observedAt"
                        ).exists()
                );
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class
    })
    @EntityScan(
            basePackageClasses = {
                    OperationalIncidentJpaEntity.class,
                    IncidentTimelineJpaEntity.class
            }
    )
    @EnableJpaRepositories(
            basePackageClasses = {
                    OperationalIncidentSpringDataRepository.class
            }
    )
    @Import({
            AdministrationQueryController.class,
            IncidentQueryController.class,
            AdministrationQueryService.class,
            IncidentQueryService.class,
            SpringConfigurationAdministrationSettingsAdapter.class,
            OperationalIncidentRepositoryAdapter.class,
            TestPortsConfiguration.class
    })
    static class TestApplication {
    }

    @TestConfiguration(
            proxyBeanMethods = false
    )
    static class TestPortsConfiguration {

        @Bean
        IntegrationHealthQueryPort integrationHealthQueryPort() {
            return List::of;
        }

        @Bean
        TimeProvider timeProvider() {
            return new SystemTimeProvider();
        }
    }
}
