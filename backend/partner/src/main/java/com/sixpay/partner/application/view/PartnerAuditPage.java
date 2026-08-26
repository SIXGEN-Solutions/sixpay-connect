package com.sixpay.partner.application.view;

import java.util.List;

public record PartnerAuditPage(
        List<PartnerAuditView> items,
        int page,
        int size,
        long totalElements,
        long totalPages
) {

    public PartnerAuditPage {
        items = List.copyOf(items);
    }
}
