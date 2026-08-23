package com.sixpay.customer.management.linking;

import com.sixpay.customer.management.application.service.ObservedCustomerLinkService;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.ObservedCustomerLinkRepository;
import com.sixpay.customer.observation.application.port.input.query.GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.query.ObservedCustomerDetailView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ObservedCustomerLinkServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void explicitlyLinksExistingObservationToExistingMasterCustomer() {
        GetObservedCustomerUseCase observed =
                mock(GetObservedCustomerUseCase.class);
        CustomerRepository customers =
                mock(CustomerRepository.class);
        ObservedCustomerLinkRepository links =
                mock(ObservedCustomerLinkRepository.class);

        UUID observedCustomerId = UUID.randomUUID();
        CustomerId customerId =
                new CustomerId(UUID.randomUUID());

        when(observed.get(any()))
                .thenReturn(mock(ObservedCustomerDetailView.class));
        when(customers.existsById(customerId))
                .thenReturn(true);
        when(links.findByObservedCustomerId(
                observedCustomerId
        )).thenReturn(Optional.empty());
        when(links.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ObservedCustomerLinkService service =
                new ObservedCustomerLinkService(
                        observed,
                        customers,
                        links
                );

        ObservedCustomerLink link =
                service.link(
                        observedCustomerId,
                        customerId,
                        "admin-user",
                        "corr-1",
                        "manual correlation confirmed",
                        NOW
                );

        assertThat(link.customerId())
                .isEqualTo(customerId);

        verify(observed).get(any());
        verify(customers).existsById(customerId);
        verify(links).save(link);
        verify(customers, never()).save(any());
    }

    @Test
    void neverCreatesMasterDataWhenTargetCustomerDoesNotExist() {
        GetObservedCustomerUseCase observed =
                mock(GetObservedCustomerUseCase.class);
        CustomerRepository customers =
                mock(CustomerRepository.class);
        ObservedCustomerLinkRepository links =
                mock(ObservedCustomerLinkRepository.class);

        CustomerId missingCustomer =
                new CustomerId(UUID.randomUUID());

        when(observed.get(any()))
                .thenReturn(mock(ObservedCustomerDetailView.class));
        when(customers.existsById(missingCustomer))
                .thenReturn(false);

        ObservedCustomerLinkService service =
                new ObservedCustomerLinkService(
                        observed,
                        customers,
                        links
                );

        assertThatThrownBy(() ->
                service.link(
                        UUID.randomUUID(),
                        missingCustomer,
                        "admin-user",
                        "corr-1",
                        "manual correlation",
                        NOW
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "customer not found"
                );

        verify(customers, never()).save(any());
        verifyNoInteractions(links);
    }

    @Test
    void refusesSilentRelinkToDifferentCustomer() {
        GetObservedCustomerUseCase observed =
                mock(GetObservedCustomerUseCase.class);
        CustomerRepository customers =
                mock(CustomerRepository.class);
        ObservedCustomerLinkRepository links =
                mock(ObservedCustomerLinkRepository.class);

        UUID observedCustomerId = UUID.randomUUID();
        CustomerId firstCustomer =
                new CustomerId(UUID.randomUUID());
        CustomerId secondCustomer =
                new CustomerId(UUID.randomUUID());

        ObservedCustomerLink current =
                ObservedCustomerLink.create(
                        observedCustomerId,
                        firstCustomer,
                        "admin-user",
                        "corr-1",
                        "first correlation",
                        NOW
                );

        when(observed.get(any()))
                .thenReturn(mock(ObservedCustomerDetailView.class));
        when(customers.existsById(secondCustomer))
                .thenReturn(true);
        when(links.findByObservedCustomerId(
                observedCustomerId
        )).thenReturn(Optional.of(current));

        ObservedCustomerLinkService service =
                new ObservedCustomerLinkService(
                        observed,
                        customers,
                        links
                );

        assertThatThrownBy(() ->
                service.link(
                        observedCustomerId,
                        secondCustomer,
                        "admin-user",
                        "corr-2",
                        "attempted replacement",
                        NOW.plusSeconds(1)
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "unlink first"
                );

        verify(links, never()).save(any());
    }
}
