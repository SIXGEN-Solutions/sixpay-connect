package com.sixpay.partner.infrastructure.persistence;

import com.sixpay.partner.application.port.output.PartnerCatalog;
import com.sixpay.partner.application.view.PartnerPage;
import com.sixpay.partner.application.view.PartnerSummaryView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Repository
public class PartnerCatalogAdapter implements PartnerCatalog {

    private static final Sort CATALOG_SORT = Sort.by(
            Sort.Order.asc("legalName"),
            Sort.Order.asc("id")
    );

    private final PartnerSpringDataRepository repository;

    public PartnerCatalogAdapter(PartnerSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public PartnerPage findPage(int page, int size) {
        var entityPage = repository.findAll(
                PageRequest.of(page, size, CATALOG_SORT)
        );

        if (entityPage.isEmpty()) {
            return new PartnerPage(
                    List.of(),
                    page,
                    size,
                    entityPage.getTotalElements(),
                    entityPage.getTotalPages()
            );
        }

        var orderedIds = entityPage.getContent().stream()
                .map(PartnerJpaEntity::id)
                .toList();

        var detailsById = new HashMap<UUID, PartnerJpaEntity>();
        repository.findCatalogDetailsByIdIn(orderedIds)
                .forEach(entity -> detailsById.put(entity.id(), entity));

        var items = orderedIds.stream()
                .map(detailsById::get)
                .map(PartnerCatalogAdapter::toView)
                .toList();

        return new PartnerPage(
                items,
                page,
                size,
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    private static PartnerSummaryView toView(PartnerJpaEntity entity) {
        if (entity == null) {
            throw new IllegalStateException(
                    "Partner catalog page could not be hydrated"
            );
        }

        return new PartnerSummaryView(
                entity.id(),
                entity.legalName(),
                entity.technicalContactName(),
                entity.technicalContactEmail(),
                entity.authorizedTransactionTypes(),
                entity.status(),
                entity.createdAt(),
                entity.updatedAt()
        );
    }
}
