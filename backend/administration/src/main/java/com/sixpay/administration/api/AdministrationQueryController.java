package com.sixpay.administration.api;

import com.sixpay.administration.api.dto.AdministrationOverviewResponse;
import com.sixpay.administration.api.dto.AdministrationSettingsResponse;
import com.sixpay.administration.api.dto.IntegrationStatusResponse;
import com.sixpay.administration.application.port.input.AdministrationQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/administration")
@PreAuthorize("hasRole('ADMIN')")
@Tag(
        name = "Administration",
        description =
                "Read-only operational Administration query API"
)
@SecurityRequirement(name = "bearerAuth")
public class AdministrationQueryController {

    private static final String CORRELATION_ID =
            "X-Correlation-ID";

    private final AdministrationQueryUseCase useCase;

    public AdministrationQueryController(
            AdministrationQueryUseCase useCase
    ) {
        this.useCase =
                Objects.requireNonNull(useCase);
    }

    @GetMapping("/overview")
    @Operation(
            summary = "Get Administration operational overview",
            description =
                    "Returns the effective operational settings "
                            + "and observable integration-health projections."
    )
    public AdministrationOverviewResponse overview(
            @RequestHeader(CORRELATION_ID)
            UUID correlationId
    ) {
        return AdministrationOverviewResponse.from(
                useCase.overview()
        );
    }

    @GetMapping("/settings")
    @Operation(
            summary = "Get effective Administration settings",
            description =
                    "Returns only operational settings backed "
                            + "by real SIXPAY runtime configuration."
    )
    public AdministrationSettingsResponse settings(
            @RequestHeader(CORRELATION_ID)
            UUID correlationId
    ) {
        return AdministrationSettingsResponse.from(
                useCase.settings()
        );
    }

    @GetMapping("/integrations")
    @Operation(
            summary = "Get observable integration health",
            description =
                    "Returns the bounded set of integration-health "
                            + "states that SIXPAY can observe at runtime."
    )
    public List<IntegrationStatusResponse> integrations(
            @RequestHeader(CORRELATION_ID)
            UUID correlationId
    ) {
        return useCase.integrations()
                .stream()
                .map(IntegrationStatusResponse::from)
                .toList();
    }
}
