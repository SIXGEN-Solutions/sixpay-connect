package com.sixpay.bootstrap.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "SIXPAY CONNECT API",
                version = "v1",
                description = "Official REST API of SIXPAY CONNECT",
                contact = @Contact(
                        name = "SIXGEN Solutions",
                        url = "https://github.com/SIXGEN-Solutions"
                )
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description =
                "OAuth2 access token issued for SIXPAY CONNECT"
)
public class OpenApiConfiguration {

    @Bean
    GroupedOpenApi partnerOpenApi() {
        return GroupedOpenApi.builder()
                .group("partner")
                .displayName("Partner API")
                .pathsToMatch(
                        "/api/v1/partners/**"
                )
                .build();
    }

    /*
    @Bean
    GroupedOpenApi paymentOpenApi() {
        return GroupedOpenApi.builder()
                .group("payment")
                .displayName("Payment API")
                .pathsToMatch(
                        "/v1/payments",
                        "/v1/payments/**",
                        "/internal/api/v1/payments",
                        "/internal/api/v1/payments/**"
                )
                .build();
    }
    */

    @Bean
    GroupedOpenApi customerOpenApi() {
        return GroupedOpenApi.builder()
                .group("customer")
                .displayName("Customer API")
                .pathsToMatch(
                        "/internal/api/v1/customers",
                        "/internal/api/v1/customers/**",
                        "/internal/api/v1/subscriptions",
                        "/internal/api/v1/subscriptions/**",
                        "/internal/api/v1/observed-customers",
                        "/internal/api/v1/observed-customers/**",
                        "/internal/api/v1/customer-audit-records",
                        "/internal/api/v1/customer-audit-records/**"
                )
                .build();
    }

    @Bean
    GroupedOpenApi paymentOpenApi() {
        return GroupedOpenApi.builder()
                .group("payment")
                .displayName("Payment API")
                .pathsToMatch(
                        "/v1/payments",
                        "/v1/payments/**",
                        "/internal/api/v1/payments",
                        "/internal/api/v1/payments/{paymentId}"
                )
                .pathsToExclude(
                        "/internal/api/v1/payments/*/timeline"
                )
                .build();
    }

    @Bean
    GroupedOpenApi administrationOpenApi() {
        return GroupedOpenApi.builder()
                .group("administration")
                .displayName("User Administration API")
                .pathsToMatch(
                        "/internal/api/v1/administration/users",
                        "/internal/api/v1/administration/users/**"
                )
                .build();
    }

    @Bean
    GroupedOpenApi reportingOpenApi() {
        return GroupedOpenApi.builder()
                .group("reporting")
                .displayName("Payment Audit API")
                .pathsToMatch(
                        "/internal/api/v1/payments/*/timeline",
                        "/internal/api/v1/payment-audit-records",
                        "/internal/api/v1/payment-audit-records/**",
                        "/internal/api/v1/payment-audit-exports",
                        "/internal/api/v1/payment-audit-exports/**"
                )
                .build();
    }
}
