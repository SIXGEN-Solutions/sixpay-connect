package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.Customer;

import java.util.List;

public record CustomerSearchPage(
        List<Customer> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
    public CustomerSearchPage {
        content = List.copyOf(content);
    }
}
