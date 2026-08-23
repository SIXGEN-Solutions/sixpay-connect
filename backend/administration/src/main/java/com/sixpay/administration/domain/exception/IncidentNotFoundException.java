package com.sixpay.administration.domain.exception;

import com.sixpay.administration.domain.model.IncidentId;

public class IncidentNotFoundException
        extends RuntimeException {

    public IncidentNotFoundException(
            IncidentId incidentId
    ) {
        super(
                "Operational incident not found: "
                        + incidentId.value()
        );
    }
}
