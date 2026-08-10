package com.sixpay.security.configuration;

import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authentication.SecurityContextCurrentUserProvider;
import com.sixpay.security.jwt.SixpayJwtAuthoritiesConverter;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SixpaySecurityAutoConfigurationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "sixpay.security.mode=oauth2"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SixpaySecurityAutoConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private SixpayJwtAuthoritiesConverter authoritiesConverter;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createsDefaultSecurityBeans() {
        assertThat(currentUserProvider)
                .isInstanceOf(SecurityContextCurrentUserProvider.class);

        assertThat(authoritiesConverter).isNotNull();
        assertThat(jwtAuthenticationConverter).isNotNull();
    }

    @Test
    void permitsHealthEndpointWithoutAuthentication()
            throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("UP"));
    }

    @Test
    void rejectsProtectedEndpointWithoutAuthentication()
            throws Exception {

        mockMvc.perform(get("/secured"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsProtectedEndpointWithJwtAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/secured")
                                .with(jwt())
                )
                .andExpect(status().isOk())
                .andExpect(content().string("SECURED"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        TestController securityTestController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/actuator/health")
        ResponseEntity<String> health() {
            return ResponseEntity.ok("UP");
        }

        @GetMapping("/secured")
        ResponseEntity<String> secured() {
            return ResponseEntity.ok("SECURED");
        }
    }
}
