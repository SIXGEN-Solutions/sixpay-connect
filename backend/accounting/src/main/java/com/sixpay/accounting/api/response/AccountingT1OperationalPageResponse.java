package com.sixpay.accounting.api.response;

import com.sixpay.accounting.application.port.input.AccountingT1OperationalQueryUseCase;

import java.util.List;

public record AccountingT1OperationalPageResponse(
        List<AccountingT1OperationalResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AccountingT1OperationalPageResponse from(
            AccountingT1OperationalQueryUseCase.Page result
    ) {
        return new AccountingT1OperationalPageResponse(
                result.content().stream()
                        .map(AccountingT1OperationalResponse::from)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
