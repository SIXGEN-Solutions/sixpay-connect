package com.sixpay.partner.cucumber;

import com.sixpay.partner.configuration.PartnerModuleConfiguration;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authentication.SecurityContextCurrentUserProvider;
import com.sixpay.security.configuration.SixpaySecurityAutoConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@CucumberContextConfiguration
@SpringBootTest(
        classes = CucumberSpringConfiguration.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = SixpaySecurityAutoConfiguration.class)
    @EnableMethodSecurity
    @ImportAutoConfiguration(PartnerModuleConfiguration.class)
    static class TestApplication {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new SecurityContextCurrentUserProvider();
        }

        @Bean
        SecurityAuditPort securityAuditPort() {
            return Mockito.mock(SecurityAuditPort.class);
        }
    }
}
