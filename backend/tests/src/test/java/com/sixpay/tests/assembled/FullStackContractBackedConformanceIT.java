package com.sixpay.tests.assembled;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = FullStackContractBackedConformanceIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("assembled-test")
@Testcontainers
@EnabledIfSystemProperty(
        named = "sixpay.assembled.tests",
        matches = "true"
)
class FullStackContractBackedConformanceIT {

    private static final String CORRELATION =
            "X-Correlation-Id";

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse(
                            "postgres:15-alpine"
                    )
            )
                    .withDatabaseName(
                            "sixpay_full_stack_conformance"
                    )
                    .withUsername(
                            "sixpay"
                    )
                    .withPassword(
                            "sixpay-test"
                    );

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
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RequestMappingHandlerMapping mappings;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void requiredContractBackedEndpointsAreMapped() {

        Set<String> patterns =
                mappings
                        .getHandlerMethods()
                        .keySet()
                        .stream()
                        .flatMap(
                                info ->
                                        info
                                                .getPatternValues()
                                                .stream()
                        )
                        .collect(
                                Collectors.toSet()
                        );

        assertThat(patterns).contains(
                "/api/v1/partners",
                "/internal/api/v1/payments",
                "/internal/api/v1/customers",
                "/internal/api/v1/observed-customers",
                "/internal/api/v1/payment-audit-records",
                "/internal/api/v1/administration/users"
        );
    }

    @Test
    void repositoryInfrastructureIsActuallyRegistered() {

        List<String> requiredTypes =
                List.of(
                        "com.sixpay.partner.infrastructure.persistence."
                                + "PartnerSpringDataRepository",
                        "com.sixpay.customer.management.infrastructure."
                                + "persistence.CustomerSpringDataRepository"
                );

        for (String typeName : requiredTypes) {
            assertBeanForType(typeName);
        }

        Integer migrationCount =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*)
                          FROM sixpay.flyway_schema_history
                         WHERE success = TRUE
                        """,
                        Integer.class
                );

        assertThat(migrationCount)
                .isNotNull()
                .isGreaterThan(0);
    }

    @Test
    @WithMockUser(
            username = "fs-conformance",
            authorities = {
                    "ROLE_ADMIN",
                    "SCOPE_partner.read",
                    "SCOPE_payment.read",
                    "SCOPE_payment.audit.read",
                    "SCOPE_observed-customer.read",
                    "SCOPE_customer.read",
                    "SCOPE_customer.audit.read",
                    "SCOPE_subscription.read"
            }
    )
    void representativeReadPathsExecuteAgainstPostgreSql()
            throws Exception {

        String correlationId =
                "11111111-1111-4111-8111-111111111111";

        mvc.perform(
                        get("/api/v1/partners")
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
                                        correlationId
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mvc.perform(
                        get("/internal/api/v1/payments")
                                .param(
                                        "size",
                                        "20"
                                )
                                .header(
                                        CORRELATION,
                                        correlationId
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mvc.perform(
                        get("/internal/api/v1/customers")
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
                                        correlationId
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "observed-customers"
                        )
                                .param(
                                        "size",
                                        "20"
                                )
                                .header(
                                        CORRELATION,
                                        correlationId
                                )
                )
                .andExpect(
                        status().isOk()
                );

        Instant to =
                Instant.now()
                        .truncatedTo(
                                ChronoUnit.SECONDS
                        );

        Instant from =
                to.minus(
                        1,
                        ChronoUnit.DAYS
                );

        mvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "payment-audit-records"
                        )
                                .param(
                                        "occurredFrom",
                                        from.toString()
                                )
                                .param(
                                        "occurredTo",
                                        to.toString()
                                )
                                .param(
                                        "size",
                                        "20"
                                )
                                .header(
                                        CORRELATION,
                                        correlationId
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "administration/users"
                        )
                                .header(
                                        CORRELATION,
                                        correlationId
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private void assertBeanForType(
            String typeName
    ) {

        try {

            Class type =
                    Class.forName(
                            typeName
                    );

            assertThat(
                    context.getBeansOfType(type)
            )
                    .as(
                            typeName
                                    + " must be registered "
                                    + "in assembled context"
                    )
                    .isNotEmpty();

        } catch (ClassNotFoundException exception) {

            throw new AssertionError(
                    "Required persistence type "
                            + "is absent: "
                            + typeName,
                    exception
            );
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = {
                    "com.sixpay.common",
                    "com.sixpay.partner",
                    "com.sixpay.customer",
                    "com.sixpay.payment",
                    "com.sixpay.accounting",
                    "com.sixpay.reporting",
                    "com.sixpay.notification",
                    "com.sixpay.administration",
                    "com.sixpay.security",
                    "com.sixpay.integration"
            }
    )
    static class TestApplication {
    }
}