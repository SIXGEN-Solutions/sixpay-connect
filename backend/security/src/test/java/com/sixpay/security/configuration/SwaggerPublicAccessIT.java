package com.sixpay.security.configuration;

import com.sixpay.security.application.port.out.SecurityAuditPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SwaggerPublicAccessIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "sixpay.security.authentication.local.enabled=false",
                "sixpay.security.authentication.oidc.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerPublicAccessIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityAuditPort securityAuditPort;

    @Test
    void swaggerUiIndexIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string("SWAGGER_UI"));
    }

    @Test
    void swaggerUiCompatibilityPathIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(content().string("SWAGGER_UI_COMPAT"));
    }

    @Test
    void openApiDescriptorIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string("OPENAPI"));
    }

    @Test
    void openApiSwaggerConfigurationIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(content().string("OPENAPI_CONFIG"));
    }

    @Test
    void businessEndpointRemainsProtected() throws Exception {
        mockMvc.perform(get("/test-secured"))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    DataJpaRepositoriesAutoConfiguration.class
            }
    )
    static class TestApplication {

        @Bean
        SwaggerBoundaryController swaggerBoundaryController() {
            return new SwaggerBoundaryController();
        }
    }

    @RestController
    static class SwaggerBoundaryController {

        @GetMapping("/swagger-ui/index.html")
        ResponseEntity<String> swaggerUi() {
            return ResponseEntity.ok("SWAGGER_UI");
        }

        @GetMapping("/swagger-ui.html")
        ResponseEntity<String> swaggerUiCompatibility() {
            return ResponseEntity.ok("SWAGGER_UI_COMPAT");
        }

        @GetMapping("/v3/api-docs")
        ResponseEntity<String> openApi() {
            return ResponseEntity.ok("OPENAPI");
        }

        @GetMapping("/v3/api-docs/swagger-config")
        ResponseEntity<String> openApiConfig() {
            return ResponseEntity.ok("OPENAPI_CONFIG");
        }

        @GetMapping("/test-secured")
        ResponseEntity<String> secured() {
            return ResponseEntity.ok("SECURED");
        }
    }
}
