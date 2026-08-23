package com.sixpay.administration.application.service;

import com.sixpay.administration.application.port.input.IncidentQueryUseCase;
import com.sixpay.administration.domain.exception.IncidentNotFoundException;
import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.OperationalIncident;
import com.sixpay.administration.domain.repository.IncidentSearchCriteria;
import com.sixpay.administration.domain.repository.IncidentSearchPage;
import com.sixpay.administration.domain.repository.OperationalIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class IncidentQueryService
        implements IncidentQueryUseCase {

    private final OperationalIncidentRepository repository;

    public IncidentQueryService(
            OperationalIncidentRepository repository
    ) {
        this.repository =
                Objects.requireNonNull(repository);
    }

    @Override
    public IncidentSearchPage search(
            IncidentSearchCriteria criteria
    ) {
        return repository.search(
                Objects.requireNonNull(criteria)
        );
    }

    @Override
    public OperationalIncident get(
            IncidentId incidentId
    ) {
        return repository.findById(
                        Objects.requireNonNull(incidentId)
                )
                .orElseThrow(
                        () ->
                                new IncidentNotFoundException(
                                        incidentId
                                )
                );
    }
}
