package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.repository.CustomerSearchPage;

import java.util.List;

public record CustomerPageResponse(
        List<CustomerResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
    public static CustomerPageResponse from(
            CustomerSearchPage page
    ) {
        return new CustomerPageResponse(
                page.content()
                        .stream()
                        .map(CustomerResponse::from)
                        .toList(),
                page.totalElements(),
                page.totalPages(),
                page.page(),
                page.size(),
                page.first(),
                page.last()
        );
    }
}
