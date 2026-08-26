package com.sixpay.accounting.api.response;

import com.sixpay.accounting.application.port.input.AccountingBatchQueryUseCase;

import java.util.List;

public record AccountingBatchPageResponse(
        List<AccountingBatchSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public AccountingBatchPageResponse {
        content = List.copyOf(content);
    }

    public static AccountingBatchPageResponse from(
            AccountingBatchQueryUseCase.AccountingBatchPage page
    ) {
        return new AccountingBatchPageResponse(
                page.content().stream()
                        .map(AccountingBatchSummaryResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}
