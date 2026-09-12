package com.sixpay.accounting.api.response;

import com.sixpay.accounting.application.port.input.TfjOperationalQueryUseCase;

import java.util.List;

public record TfjOperationalPageResponse(
        List<TfjOperationalResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static TfjOperationalPageResponse from(
            TfjOperationalQueryUseCase.Page source
    ) {
        return new TfjOperationalPageResponse(
                source.content().stream()
                        .map(TfjOperationalResponse::from)
                        .toList(),
                source.page(),
                source.size(),
                source.totalElements(),
                source.totalPages()
        );
    }
}
