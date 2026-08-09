package com.sixpay.partner.application.view;

import java.util.List;

public record PartnerPage(
        List<PartnerSummaryView> items,
        int page,
        int size,
        long totalElements,
        long totalPages
) {

    public PartnerPage {
        items = List.copyOf(items);
    }
}
