package com.sixpay.administration.domain.repository;

import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.OperationalIncident;

import java.util.Optional;

public interface OperationalIncidentRepository {

    Optional<OperationalIncident> findById(
            IncidentId incidentId
    );

    IncidentSearchPage search(
            IncidentSearchCriteria criteria
    );
}
