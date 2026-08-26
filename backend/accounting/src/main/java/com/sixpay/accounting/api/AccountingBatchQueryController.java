package com.sixpay.accounting.api;

import com.sixpay.accounting.api.response.AccountingBatchDetailResponse;
import com.sixpay.accounting.api.response.AccountingBatchPageResponse;
import com.sixpay.accounting.application.port.input.AccountingBatchQueryUseCase;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/accounting-batches")
@Validated
@Tag(name = "Accounting", description = "Internal Accounting batch query API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
public class AccountingBatchQueryController {

    private final AccountingBatchQueryUseCase query;

    public AccountingBatchQueryController(AccountingBatchQueryUseCase query) {
        this.query = query;
    }

    @GetMapping
    @Operation(summary = "Search accounting batches")
    public AccountingBatchPageResponse search(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessDate,
            @RequestParam(required = false)
            AccountingBatchStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        return AccountingBatchPageResponse.from(
                query.search(businessDate, status, page, size)
        );
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get accounting batch detail")
    public AccountingBatchDetailResponse findById(
            @PathVariable UUID batchId
    ) {
        return AccountingBatchDetailResponse.from(
                query.findById(new AccountingBatchId(batchId))
        );
    }
}
