package com.sixpay.accounting.api;

import com.sixpay.accounting.api.response.AccountingT1OperationalPageResponse;
import com.sixpay.accounting.api.response.AccountingT1OperationalResponse;
import com.sixpay.accounting.application.port.input.AccountingT1OperationalQueryUseCase;
import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
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
@RequestMapping("/internal/api/v1/accounting-t1-operations")
@Validated
@PreAuthorize(
        "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR') "
                + "and hasAuthority('SCOPE_accounting.read')"
)
public class AccountingT1OperationalQueryController {

    private final AccountingT1OperationalQueryUseCase query;

    public AccountingT1OperationalQueryController(
            AccountingT1OperationalQueryUseCase query
    ) {
        this.query = query;
    }

    @GetMapping
    public AccountingT1OperationalPageResponse search(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessDate,
            @RequestParam(required = false)
            AccountingT1OperationalCandidateStatus status,
            @RequestParam(required = false)
            String paymentReference,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        return AccountingT1OperationalPageResponse.from(
                query.search(
                        businessDate,
                        status,
                        paymentReference,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{candidateId}")
    public AccountingT1OperationalResponse findById(
            @PathVariable UUID candidateId
    ) {
        return AccountingT1OperationalResponse.from(
                query.findByCandidateId(candidateId)
        );
    }
}
