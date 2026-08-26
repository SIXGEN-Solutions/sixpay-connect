package com.sixpay.accounting.infrastructure.accountingapi.mapper;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.domain.model.AccountingProviderBatchResult;
import com.sixpay.accounting.domain.model.AccountingProviderItemResult;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchRequestDto;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchResponseDto;

import java.util.List;
import java.util.Objects;

public final class AccountingApiMapper {

    public AccountingBatchRequestDto toRequest(
            AccountingBatch batch
    ) {
        Objects.requireNonNull(batch, "batch");

        return new AccountingBatchRequestDto(
                batch.batchId().value(),
                batch.idempotencyKey().value(),
                batch.businessDate(),
                batch.financialInstitutionCode(),
                batch.createdAt(),
                batch.items().stream()
                        .map(item ->
                                new AccountingBatchRequestDto.Item(
                                        item.paymentId(),
                                        item.publicPaymentReference(),
                                        item.partnerId(),
                                        item.amount(),
                                        item.currency().getCurrencyCode(),
                                        item.paymentOccurredAt(),
                                        item.paymentBusinessDate(),
                                        item.bankPostingReference(),
                                        item.tresorPayStatus(),
                                        item.tresorPayStatusCheckedAt()
                                )
                        )
                        .toList()
        );
    }

    public AccountingProviderBatchResult toResult(
            AccountingBatchResponseDto response
    ) {
        Objects.requireNonNull(response, "response");

        List<AccountingProviderItemResult> items =
                response.items() == null
                        ? List.of()
                        : response.items()
                        .stream()
                        .map(item ->
                                new AccountingProviderItemResult(
                                        item.paymentId(),
                                        AccountingBatchItemStatus.valueOf(
                                                item.status()
                                        ),
                                        item.providerItemReference(),
                                        item.rejectionCode()
                                )
                        )
                        .toList();

        return new AccountingProviderBatchResult(
                new AccountingBatchId(
                        response.batchId()
                ),
                new AccountingBatchIdempotencyKey(
                        response.idempotencyKey()
                ),
                AccountingBatchStatus.valueOf(
                        response.status()
                ),
                response.providerBatchReference(),
                response.processedAt(),
                items
        );
    }
}
