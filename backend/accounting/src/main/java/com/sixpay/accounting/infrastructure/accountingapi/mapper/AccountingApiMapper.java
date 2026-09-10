package com.sixpay.accounting.infrastructure.accountingapi.mapper;

import com.sixpay.accounting.application.exception.AccountingProviderInvalidResponseException;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemEntry;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.domain.model.AccountingProviderBatchResult;
import com.sixpay.accounting.domain.model.AccountingProviderItemResult;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchRequestDto;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchResponseDto;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AccountingApiMapper {

    public AccountingBatchRequestDto toRequest(AccountingBatch batch) {
        Objects.requireNonNull(batch, "batch");

        return new AccountingBatchRequestDto(
                batch.batchId().value(),
                batch.businessDate(),
                batch.items().stream()
                        .map(this::toRequestItem)
                        .toList()
        );
    }

    public AccountingProviderBatchResult toResult(
            AccountingBatchResponseDto response,
            AccountingBatch batch
    ) {
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(batch, "batch");

        Map<String, AccountingBatchItem> byPublicReference =
                batch.items().stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        AccountingBatchItem::publicPaymentReference,
                                        Function.identity()
                                )
                        );

        var items = response.items().stream()
                .map(item -> {
                    AccountingBatchItem batchItem =
                            byPublicReference.get(item.paymentReference());

                    if (batchItem == null) {
                        throw invalid(
                                "Accounting API response contains unknown paymentReference: "
                                        + item.paymentReference()
                        );
                    }

                    return new AccountingProviderItemResult(
                            batchItem.paymentId(),
                            mapItemStatus(item.status()),
                            item.providerItemReference(),
                            item.rejectionCode()
                    );
                })
                .toList();

        return new AccountingProviderBatchResult(
                batch.batchId(),
                batch.idempotencyKey(),
                mapBatchStatus(response.status()),
                response.providerBatchReference(),
                response.processedAt(),
                items
        );
    }

    public AccountingProviderBatchResult toLookupResult(
            AccountingBatchResponseDto response,
            AccountingBatchIdempotencyKey idempotencyKey,
            Map<String, UUID> paymentIdsByReference
    ) {
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(paymentIdsByReference, "paymentIdsByReference");

        var items = response.items().stream()
                .map(item -> {
                    UUID paymentId =
                            paymentIdsByReference.get(item.paymentReference());

                    if (paymentId == null) {
                        throw invalid(
                                "Accounting API response contains unknown paymentReference: "
                                        + item.paymentReference()
                        );
                    }

                    return new AccountingProviderItemResult(
                            paymentId,
                            mapItemStatus(item.status()),
                            item.providerItemReference(),
                            item.rejectionCode()
                    );
                })
                .toList();

        return new AccountingProviderBatchResult(
                new AccountingBatchId(response.batchId()),
                idempotencyKey,
                mapBatchStatus(response.status()),
                response.providerBatchReference(),
                response.processedAt(),
                items
        );
    }

    private AccountingBatchRequestDto.Item toRequestItem(
            AccountingBatchItem item
    ) {
        if (!item.hasFinancialSnapshotEvidence()) {
            throw new IllegalArgumentException(
                    "T1.5 provider submission requires finalized T0 financial snapshot evidence"
            );
        }

        if (item.bankPostingReference() == null
                || item.bankPostingReference().isBlank()) {
            throw new IllegalArgumentException(
                    "T1.5 provider submission requires T0 bank reference"
            );
        }

        return new AccountingBatchRequestDto.Item(
                item.publicPaymentReference(),
                item.financialSnapshotId(),
                item.bankPostingReference(),
                item.entries().stream()
                        .sorted(Comparator.comparingInt(AccountingBatchItemEntry::sequence))
                        .map(this::toRequestEntry)
                        .toList()
        );
    }

    private AccountingBatchRequestDto.Entry toRequestEntry(
            AccountingBatchItemEntry entry
    ) {
        return new AccountingBatchRequestDto.Entry(
                entry.sequence(),
                splitAccountReference(entry.accountReference()),
                entry.currency().getNumericCodeAsString(),
                entry.amount(),
                mapDirection(entry.direction())
        );
    }

    private static AccountingBatchRequestDto.Account splitAccountReference(
            String accountReference
    ) {
        String[] parts = accountReference.split("-", -1);
        if (parts.length != 3
                || Arrays.stream(parts).anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Core Banking account reference must use age-ncp-clc format: "
                            + accountReference
            );
        }

        return new AccountingBatchRequestDto.Account(
                parts[0],
                parts[1],
                parts[2]
        );
    }

    private static String mapDirection(String direction) {
        return switch (direction) {
            case "DEBIT" -> "D";
            case "CREDIT" -> "C";
            default -> throw new IllegalArgumentException(
                    "Unsupported accounting direction: " + direction
            );
        };
    }

    private static AccountingBatchStatus mapBatchStatus(
            String providerStatus
    ) {
        return switch (required(providerStatus, "batch status")) {
            case "COMPLETED" -> AccountingBatchStatus.COMPLETED;
            case "ACCEPTED", "PROCESSING" -> AccountingBatchStatus.NOT_COMPLETED;
            default -> throw invalid(
                    "Unsupported provider batch status: " + providerStatus
            );
        };
    }

    private static AccountingBatchItemStatus mapItemStatus(
            String providerStatus
    ) {
        return switch (required(providerStatus, "item status")) {
            case "SUCCESS" -> AccountingBatchItemStatus.COMPLETED;
            case "FAILED" -> AccountingBatchItemStatus.REJECTED;
            case "UNKNOWN" -> AccountingBatchItemStatus.RECONCILIATION_REQUIRED;
            default -> throw invalid(
                    "Unsupported provider item status: " + providerStatus
            );
        };
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw invalid(name + " is required");
        }
        return value.strip();
    }

    private static AccountingProviderInvalidResponseException invalid(
            String message
    ) {
        return new AccountingProviderInvalidResponseException(message, null);
    }
}
