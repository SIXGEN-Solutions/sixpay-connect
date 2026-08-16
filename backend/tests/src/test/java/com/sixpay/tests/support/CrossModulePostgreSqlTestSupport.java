package com.sixpay.tests.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class CrossModulePostgreSqlTestSupport {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:15-alpine")
            )
                    .withDatabaseName("sixpay_cross_module")
                    .withUsername("sixpay")
                    .withPassword("sixpay-test");

    @DynamicPropertySource
    static void registerPostgreSqlProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected static String jdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    protected static String jdbcUsername() {
        return POSTGRES.getUsername();
    }

    protected static String jdbcPassword() {
        return POSTGRES.getPassword();
    }
}
