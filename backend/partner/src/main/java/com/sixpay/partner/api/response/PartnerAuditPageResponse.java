package com.sixpay.partner.api.response;

import com.sixpay.partner.application.view.PartnerAuditPage;

import java.util.List;

/**
 * Public paginated response for the Partner audit trail.
 */
public record PartnerAuditPageResponse(
        List<PartnerAuditResponse> items,
        int page,
        int size,
        long totalElements,
        long totalPages
) {

    public PartnerAuditPageResponse {
        items = List.copyOf(items);
    }

    public static PartnerAuditPageResponse from(PartnerAuditPage page) {
        return new PartnerAuditPageResponse(
                page.items().stream()
                        .map(PartnerAuditResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}