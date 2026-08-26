package com.sixpay.customer.observation.api.controller;

import com.sixpay.customer.observation.api.dto
        .ObservedCustomerSearchPageResponse;
import com.sixpay.customer.observation.api.error
        .ObservedCustomerQueryExceptionHandler;
import com.sixpay.customer.observation.api.mapper
        .ObservedCustomerQueryApiMapper;
import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryObservation;
import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryOperation;
import com.sixpay.customer.observation.application.port.input.query
        .GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.port.input.query
        .ListObservedCustomerPaymentsUseCase;
import com.sixpay.customer.observation.application.port.input.query
        .SearchObservedCustomersUseCase;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPage;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;
import com.sixpay.customer.observation.configuration
        .ObservedCustomerObservabilityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration
        .EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

@WebMvcTest(ObservedCustomerQueryController.class)
@ContextConfiguration(classes = {
        ObservedCustomerQueryController.class,
        ObservedCustomerQueryExceptionHandler.class,
        ObservedCustomerQueryControllerTest.MethodSecurityConfiguration.class,
        ObservedCustomerQueryControllerTest.ClockConfiguration.class
})
class ObservedCustomerQueryControllerTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString(
                    "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
            );

    private static final String CORRELATION_ID =
            "11111111-1111-4111-8111-111111111111";

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-09T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchObservedCustomersUseCase searchUseCase;

    @MockitoBean
    private GetObservedCustomerUseCase getUseCase;

    @MockitoBean
    private ListObservedCustomerPaymentsUseCase paymentsUseCase;

    @MockitoBean
    private ObservedCustomerQueryApiMapper mapper;

    @MockitoBean
    private ObservedCustomerQueryObservation observation;

    @BeforeEach
    void passThroughObservedActions() {
        doAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(4);
            return action.get();
        }).when(observation).observe(
                any(ObservedCustomerQueryOperation.class),
                anyString(),
                nullable(UUID.class),
                nullable(Integer.class),
                any(),
                any()
        );
    }

    @Test
    @WithMockUser(
            username = "reader@sixpay",
            authorities = "SCOPE_observed-customer.read"
    )
    void searchAllowsPublishedReadScopeAndEchoesCorrelationId()
            throws Exception {

        ObservedCustomerSearchPage page =
                new ObservedCustomerSearchPage(
                        List.of(),
                        0,
                        false,
                        null,
                        SNAPSHOT
                );

        ObservedCustomerSearchPageResponse response =
                new ObservedCustomerSearchPageResponse(
                        List.of(),
                        0,
                        false,
                        null,
                        SNAPSHOT
                );

        when(searchUseCase.search(
                any(SearchObservedCustomersQuery.class)
        )).thenReturn(page);

        when(mapper.toResponse(page))
                .thenReturn(response);

        mockMvc.perform(
                        get("/internal/api/v1/observed-customers")
                                .header(
                                        ObservedCustomerQueryController
                                                .CORRELATION_HEADER,
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                ObservedCustomerQueryController
                                        .CORRELATION_HEADER,
                                CORRELATION_ID
                        )
                )
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.items").isArray());

        verify(searchUseCase)
                .search(any(SearchObservedCustomersQuery.class));
        verify(mapper).toResponse(page);
    }

    @Test
    @WithMockUser(
            username = "admin-without-scope@sixpay",
            roles = "ADMIN"
    )
    void searchForbidsAuthenticatedCallerWithoutReadScope()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/observed-customers")
                                .header(
                                        ObservedCustomerQueryController
                                                .CORRELATION_HEADER,
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                searchUseCase,
                mapper,
                observation
        );
    }

    @Test
    @WithMockUser(
            username = "reader@sixpay",
            authorities = "SCOPE_observed-customer.read"
    )
    void searchRejectsMissingCorrelationHeader()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/observed-customers")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(searchUseCase);
    }

    @Test
    @WithMockUser(
            username = "reader@sixpay",
            authorities = "SCOPE_observed-customer.read"
    )
    void searchRejectsMalformedCorrelationHeader()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/observed-customers")
                                .header(
                                        ObservedCustomerQueryController
                                                .CORRELATION_HEADER,
                                        "not-a-uuid"
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(searchUseCase);
    }

    @Test
    @WithMockUser(
            username = "reader@sixpay",
            authorities = "SCOPE_observed-customer.read"
    )
    void searchRejectsPageSizeAboveContractLimit()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/observed-customers")
                                .header(
                                        ObservedCustomerQueryController
                                                .CORRELATION_HEADER,
                                        CORRELATION_ID
                                )
                                .queryParam("size", "201")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(searchUseCase);
    }

    @Test
    @WithMockUser(
            username = "caller-without-scope@sixpay",
            roles = "MANAGER"
    )
    void getForbidsAuthenticatedCallerWithoutReadScope()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/internal/api/v1/observed-customers/{id}",
                                CUSTOMER_ID
                        )
                                .header(
                                        ObservedCustomerQueryController
                                                .CORRELATION_HEADER,
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(getUseCase);
    }

    @Test
    @WithMockUser(
            username = "caller-without-scope@sixpay",
            roles = "AUDITOR"
    )
    void listPaymentsForbidsAuthenticatedCallerWithoutReadScope()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/internal/api/v1/observed-customers/{id}/payments",
                                CUSTOMER_ID
                        )
                                .header(
                                        ObservedCustomerQueryController
                                                .CORRELATION_HEADER,
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentsUseCase);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        @Qualifier(
                ObservedCustomerObservabilityConfiguration
                        .OBSERVED_CUSTOMER_CLOCK
        )
        Clock observedCustomerClock() {
            return Clock.fixed(
                    SNAPSHOT,
                    ZoneOffset.UTC
            );
        }
    }
}
