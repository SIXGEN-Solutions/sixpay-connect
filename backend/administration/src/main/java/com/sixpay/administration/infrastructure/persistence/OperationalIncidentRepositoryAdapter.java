package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.OperationalIncident;
import com.sixpay.administration.domain.repository.IncidentSearchCriteria;
import com.sixpay.administration.domain.repository.IncidentSearchPage;
import com.sixpay.administration.domain.repository.OperationalIncidentRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class OperationalIncidentRepositoryAdapter
        implements OperationalIncidentRepository {

    private final OperationalIncidentSpringDataRepository
            springDataRepository;

    public OperationalIncidentRepositoryAdapter(
            OperationalIncidentSpringDataRepository
                    springDataRepository
    ) {
        this.springDataRepository =
                springDataRepository;
    }

    @Override
    public Optional<OperationalIncident> findById(
            IncidentId incidentId
    ) {
        return springDataRepository
                .findById(incidentId.value())
                .map(
                        OperationalIncidentJpaEntity::toDomain
                );
    }

    @Override
    public IncidentSearchPage search(
            IncidentSearchCriteria criteria
    ) {
        var pageable =
                PageRequest.of(
                        criteria.page(),
                        criteria.size(),
                        Sort.by(
                                Sort.Order.desc("openedAt"),
                                Sort.Order.desc("incidentId")
                        )
                );

        var result =
                springDataRepository.findAll(
                        specification(criteria),
                        pageable
                );

        return new IncidentSearchPage(
                result.getContent()
                        .stream()
                        .map(
                                OperationalIncidentJpaEntity::toDomain
                        )
                        .toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                result.isFirst(),
                result.isLast()
        );
    }

    private static Specification<
            OperationalIncidentJpaEntity
            > specification(
            IncidentSearchCriteria criteria
    ) {
        return (root, query, builder) -> {
            var predicates =
                    new ArrayList<Predicate>();

            if (criteria.severity() != null) {
                predicates.add(
                        builder.equal(
                                root.get("severity"),
                                criteria.severity()
                        )
                );
            }

            if (criteria.status() != null) {
                predicates.add(
                        builder.equal(
                                root.get("status"),
                                criteria.status()
                        )
                );
            }

            if (criteria.component() != null) {
                String pattern =
                        "%"
                                + criteria.component()
                                .toLowerCase(
                                        Locale.ROOT
                                )
                                + "%";

                predicates.add(
                        builder.like(
                                builder.lower(
                                        root.get("component")
                                ),
                                pattern
                        )
                );
            }

            return builder.and(
                    predicates.toArray(
                            Predicate[]::new
                    )
            );
        };
    }
}
