package com.sixpay.partner.infrastructure.persistence;

import com.sixpay.partner.domain.model.PartnerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PartnerCatalogAdapterTest {

    private static final UUID ALPHA_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID BETA_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000002");

    @Test
    void forwardsPageAndStableSortToSpringData() {
        var repository = mock(PartnerSpringDataRepository.class);
        var alpha = entity(ALPHA_ID, "Alpha Bank");
        var beta = entity(BETA_ID, "Beta Bank");

        var expectedPageable = PageRequest.of(
                1,
                20,
                Sort.by(
                        Sort.Order.asc("legalName"),
                        Sort.Order.asc("id")
                )
        );

        when(repository.findAll(expectedPageable))
                .thenReturn(
                        new PageImpl<>(
                                List.of(alpha, beta),
                                expectedPageable,
                                42
                        )
                );
        when(repository.findCatalogDetailsByIdIn(anyCollection()))
                .thenReturn(List.of(beta, alpha));

        var adapter = new PartnerCatalogAdapter(repository);

        var result = adapter.findPage(1, 20);

        verify(repository).findAll(expectedPageable);
        verify(repository).findCatalogDetailsByIdIn(
                List.of(ALPHA_ID, BETA_ID)
        );

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(42);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void preservesThePageOrderAfterBatchHydration() {
        var repository = mock(PartnerSpringDataRepository.class);
        var alpha = entity(ALPHA_ID, "Alpha Bank");
        var beta = entity(BETA_ID, "Beta Bank");
        var pageable = PageRequest.of(
                0,
                2,
                Sort.by(
                        Sort.Order.asc("legalName"),
                        Sort.Order.asc("id")
                )
        );

        when(repository.findAll(pageable))
                .thenReturn(
                        new PageImpl<>(
                                List.of(alpha, beta),
                                pageable,
                                2
                        )
                );

        // The hydration query is deliberately returned in the opposite order.
        when(repository.findCatalogDetailsByIdIn(anyCollection()))
                .thenReturn(List.of(beta, alpha));

        var result = new PartnerCatalogAdapter(repository)
                .findPage(0, 2);

        assertThat(result.items())
                .extracting(item -> item.id())
                .containsExactly(ALPHA_ID, BETA_ID);
    }

    @Test
    void doesNotRunTheHydrationQueryForAnEmptyPage() {
        var repository = mock(PartnerSpringDataRepository.class);
        var pageable = PageRequest.of(
                3,
                20,
                Sort.by(
                        Sort.Order.asc("legalName"),
                        Sort.Order.asc("id")
                )
        );

        when(repository.findAll(pageable))
                .thenReturn(
                        new PageImpl<>(
                                List.of(),
                                pageable,
                                41
                        )
                );

        var result = new PartnerCatalogAdapter(repository)
                .findPage(3, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(3);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);

        verify(repository).findAll(pageable);
        verifyNoMoreInteractions(repository);
    }

    private static PartnerJpaEntity entity(
            UUID id,
            String legalName
    ) {
        var entity = mock(PartnerJpaEntity.class);
        var now = Instant.parse("2026-08-08T12:00:00Z");

        when(entity.id()).thenReturn(id);
        when(entity.legalName()).thenReturn(legalName);
        when(entity.technicalContactName())
                .thenReturn(legalName + " Operations");
        when(entity.technicalContactEmail())
                .thenReturn("operations@example.test");
        when(entity.authorizedTransactionTypes())
                .thenReturn(Set.of("PAYMENT"));
        when(entity.status()).thenReturn(PartnerStatus.ACTIVE);
        when(entity.createdAt()).thenReturn(now);
        when(entity.updatedAt()).thenReturn(now);

        return entity;
    }
}
