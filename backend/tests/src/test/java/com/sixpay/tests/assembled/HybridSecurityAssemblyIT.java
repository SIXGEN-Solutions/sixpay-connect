package com.sixpay.tests.assembled;

import com.sixpay.tests.support.CrossModulePostgreSqlTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AssembledApplicationContextIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "sixpay.security.authentication.local.enabled=true",
                "sixpay.security.authentication.oidc.enabled=true",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri="
                        + "http://127.0.0.1:9/sixpay-test-jwks"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("assembled-test")
@EnabledIfSystemProperty(
        named = "sixpay.assembled.tests",
        matches = "true"
)
class HybridSecurityAssemblyIT
        extends CrossModulePostgreSqlTestSupport {

    private static final String CAPABILITIES =
            "com.sixpay.security.configuration."
                    + "AuthenticationCapabilitiesProperties";
    private static final String LOCAL_CONFIGURATION =
            "com.sixpay.security.configuration."
                    + "LocalAuthenticationConfiguration";
    private static final String LOCAL_CONTROLLER =
            "com.sixpay.security.api.controller."
                    + "LocalAuthenticationController";
    private static final String LOCAL_USE_CASE =
            "com.sixpay.security.application.port.in."
                    + "AuthenticateLocalUserUseCase";
    private static final String OIDC_ADAPTER =
            "com.sixpay.security.infrastructure.authentication.oidc."
                    + "OidcAuthenticationAdapter";
    private static final String EXTERNAL_IDENTITY_RESOLVER =
            "com.sixpay.security.application.port.out."
                    + "ExternalIdentityResolver";
    private static final String CURRENT_USER_PROVIDER =
            "com.sixpay.security.authentication."
                    + "CurrentUserProvider";
    private static final String CURRENT_SESSION_USE_CASE =
            "com.sixpay.security.application.port.in."
                    + "GetCurrentSessionUseCase";
    private static final String BEARER_FILTER =
            "org.springframework.security.oauth2.server.resource.web."
                    + "authentication.BearerTokenAuthenticationFilter";
    private static final String RESTRICTED_LOCAL_SESSION_FILTER =
            "com.sixpay.security.infrastructure.authentication.session."
                    + "RestrictedLocalSessionFilter";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void assembledApplicationStartsWithBothAuthenticationCapabilitiesEnabled()
            throws Exception {

        Object capabilities = singleBean(CAPABILITIES);

        assertThat(booleanProperty(capabilities, "localEnabled")).isTrue();
        assertThat(booleanProperty(capabilities, "oidcEnabled")).isTrue();

        assertBeanPresent(LOCAL_CONFIGURATION);
        assertBeanPresent(LOCAL_CONTROLLER);
        assertBeanPresent(LOCAL_USE_CASE);
        assertBeanPresent(OIDC_ADAPTER);
        assertBeanPresent(EXTERNAL_IDENTITY_RESOLVER);
        assertBeanPresent(CURRENT_USER_PROVIDER);
        assertBeanPresent(CURRENT_SESSION_USE_CASE);
    }

    @Test
    void hybridAssemblyUsesOneSecurityChainContainingBothMechanisms() {
        Map<String, SecurityFilterChain> chains =
                context.getBeansOfType(SecurityFilterChain.class);

        assertThat(chains)
                .as("hybrid assembly must expose one canonical security chain")
                .hasSize(1);

        SecurityFilterChain chain =
                chains.values().iterator().next();

        assertThat(
                chain.getFilters()
                        .stream()
                        .map(filter -> filter.getClass().getName())
                        .toList()
        )
                .contains(
                        BEARER_FILTER,
                        RESTRICTED_LOCAL_SESSION_FILTER
                );
    }

    @Test
    void localLoginRemainsPublicWhileBusinessApisRemainProtected()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        get("/internal/api/v1/administration/users")
                )
                .andExpect(status().isUnauthorized());
    }

    private Object singleBean(String typeName) {
        Class<?> type = requiredType(typeName);
        Map<String, ?> beans = context.getBeansOfType(type);

        assertThat(beans)
                .as(typeName + " must have exactly one assembled bean")
                .hasSize(1);

        return beans.values().iterator().next();
    }

    private void assertBeanPresent(String typeName) {
        Class<?> type = requiredType(typeName);

        assertThat(context.getBeansOfType(type))
                .as(typeName + " must participate in hybrid assembly")
                .isNotEmpty();
    }

    private static boolean booleanProperty(
            Object target,
            String methodName
    ) throws Exception {

        Method method = target.getClass().getMethod(methodName);
        return (boolean) method.invoke(target);
    }

    private static Class<?> requiredType(String typeName) {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Required hybrid-security type is absent: " + typeName,
                    exception
            );
        }
    }
}
