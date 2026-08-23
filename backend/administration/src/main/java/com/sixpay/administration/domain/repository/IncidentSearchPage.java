package com.sixpay.administration.domain.repository;

import com.sixpay.administration.domain.model.OperationalIncident;

import java.util.List;

public record IncidentSearchPage(
        List<OperationalIncident> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
    public IncidentSearchPage {
        content = List.copyOf(content);
    }
}
