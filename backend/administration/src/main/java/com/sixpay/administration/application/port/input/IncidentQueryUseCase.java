package com.sixpay.administration.application.port.input;

import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.OperationalIncident;
import com.sixpay.administration.domain.repository.IncidentSearchCriteria;
import com.sixpay.administration.domain.repository.IncidentSearchPage;

public interface IncidentQueryUseCase {

    IncidentSearchPage search(
            IncidentSearchCriteria criteria
    );

    OperationalIncident get(
            IncidentId incidentId
    );
}
