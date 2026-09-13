package com.sixpay.accounting.api;

import com.sixpay.accounting.api.response.TfjOperationalPageResponse;
import com.sixpay.accounting.api.response.TfjOperationalResponse;
import com.sixpay.accounting.application.port.input.TfjOperationalQueryUseCase;
import com.sixpay.accounting.domain.model.TfjOperationalCategory;
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
@RequestMapping("/internal/api/v1/accounting-tfj-operations")
@Validated
@PreAuthorize(
        "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR') "
                + "and hasAuthority('SCOPE_accounting.read')"
)
public class TfjOperationalQueryController {

    private final TfjOperationalQueryUseCase query;

    public TfjOperationalQueryController(
            TfjOperationalQueryUseCase query
    ) {
        this.query = query;
    }

    @GetMapping
    public TfjOperationalPageResponse search(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate businessDate,
            @RequestParam(required = false)
            TfjOperationalCategory category,
            @RequestParam(required = false)
            String paymentReference,
            @RequestParam(required = false)
            String bankPostingReference,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        return TfjOperationalPageResponse.from(
                query.search(
                        businessDate,
                        category,
                        paymentReference,
                        bankPostingReference,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{confirmationId}")
    public TfjOperationalResponse findById(
            @PathVariable UUID confirmationId
    ) {
        return TfjOperationalResponse.from(
                query.findByConfirmationId(confirmationId)
        );
    }
}
