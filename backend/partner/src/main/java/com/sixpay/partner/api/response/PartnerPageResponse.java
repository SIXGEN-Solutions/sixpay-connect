package com.sixpay.partner.api.response;

import com.sixpay.partner.application.view.PartnerPage;

import java.util.List;

public record PartnerPageResponse(
        List<PartnerSummaryResponse> items,
        int page,
        int size,
        long totalElements,
        long totalPages
) {

    public PartnerPageResponse {
        items = List.copyOf(items);
    }

    public static PartnerPageResponse from(PartnerPage page) {
        return new PartnerPageResponse(
                page.items().stream()
                        .map(PartnerSummaryResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}
