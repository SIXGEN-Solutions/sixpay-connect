package com.sixpay.accounting.api;

import com.sixpay.accounting.api.request.AccountingT1ManualExecutionRequest;
import com.sixpay.accounting.api.response.AccountingT1ManualExecutionResponse;
import com.sixpay.accounting.application.port.input.AccountingT1ManualExecutionUseCase;
import com.sixpay.common.context.CorrelationId;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@ConditionalOnProperty(
        prefix = "sixpay.accounting",
        name = {
                "api.enabled",
                "tresorpay-status.enabled"
        },
        havingValue = "true"
)
@RequestMapping("/internal/api/v1/accounting-t1-executions")
@PreAuthorize(
        "hasAnyRole('ADMIN', 'MANAGER') "
                + "and hasAuthority('SCOPE_accounting.t1.execute')"
)
public class AccountingT1ManualExecutionController {

    private static final String CORRELATION = "X-Correlation-ID";

    private final AccountingT1ManualExecutionUseCase useCase;

    public AccountingT1ManualExecutionController(
            AccountingT1ManualExecutionUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<AccountingT1ManualExecutionResponse> execute(
            @RequestHeader(CORRELATION) UUID correlationId,
            @Valid @RequestBody AccountingT1ManualExecutionRequest request
    ) {
        var result = useCase.execute(
                request.businessDate(),
                new CorrelationId(correlationId.toString())
        );

        URI location = URI.create(
                "/internal/api/v1/accounting-batches/"
                        + result.batch().batchId().value()
        );

        return ResponseEntity.ok()
                .header(CORRELATION, correlationId.toString())
                .header(HttpHeaders.LOCATION, location.toString())
                .body(AccountingT1ManualExecutionResponse.from(result));
    }
}
