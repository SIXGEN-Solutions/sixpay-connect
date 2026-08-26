package com.sixpay.administration.api;

import com.sixpay.administration.api.dto.IncidentDetailResponse;
import com.sixpay.administration.api.dto.IncidentPageResponse;
import com.sixpay.administration.application.port.input.IncidentQueryUseCase;
import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;
import com.sixpay.administration.domain.repository.IncidentSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/incidents")
@Validated
@PreAuthorize(
        "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')"
)
@Tag(
        name = "Incidents",
        description =
                "Read-only operational Incident query API"
)
@SecurityRequirement(name = "bearerAuth")
public class IncidentQueryController {

    private static final String CORRELATION_ID =
            "X-Correlation-ID";

    private final IncidentQueryUseCase useCase;

    public IncidentQueryController(
            IncidentQueryUseCase useCase
    ) {
        this.useCase =
                Objects.requireNonNull(useCase);
    }

    @GetMapping
    @Operation(
            summary = "Search operational incidents"
    )
    public IncidentPageResponse search(
            @RequestHeader(CORRELATION_ID)
            UUID correlationId,
            @RequestParam(required = false)
            IncidentSeverity severity,
            @RequestParam(required = false)
            IncidentStatus status,
            @RequestParam(required = false)
            String component,
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(200)
            int size
    ) {
        return IncidentPageResponse.from(
                useCase.search(
                        new IncidentSearchCriteria(
                                severity,
                                status,
                                component,
                                page,
                                size
                        )
                )
        );
    }

    @GetMapping("/{incidentId}")
    @Operation(
            summary = "Get operational Incident detail"
    )
    public IncidentDetailResponse get(
            @RequestHeader(CORRELATION_ID)
            UUID correlationId,
            @PathVariable
            String incidentId
    ) {
        return IncidentDetailResponse.from(
                useCase.get(
                        new IncidentId(incidentId)
                )
        );
    }
}
