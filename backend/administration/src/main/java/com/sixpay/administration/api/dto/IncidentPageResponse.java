package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.repository.IncidentSearchPage;

import java.util.List;

public record IncidentPageResponse(
        List<IncidentSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static IncidentPageResponse from(
            IncidentSearchPage page
    ) {
        return new IncidentPageResponse(
                page.content()
                        .stream()
                        .map(IncidentSummaryResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}
