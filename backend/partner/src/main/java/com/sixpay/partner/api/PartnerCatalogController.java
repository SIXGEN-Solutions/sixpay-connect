package com.sixpay.partner.api;

import com.sixpay.partner.api.response.PartnerPageResponse;
import com.sixpay.partner.application.port.in.PartnerListQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partners")
@Tag(name = "Partners", description = "Partner lifecycle and validation policy")
@SecurityRequirement(name = "bearerAuth")
public class PartnerCatalogController {

    private final PartnerListQueryUseCase query;

    public PartnerCatalogController(PartnerListQueryUseCase query) {
        this.query = query;
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('SCOPE_partner.read') "
                    + "or hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')"
    )
    @Operation(summary = "List partners with page-based pagination")
    public PartnerPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(
                    defaultValue = "" + PartnerListQueryUseCase.DEFAULT_PAGE_SIZE
            )
            @Min(1)
            @Max(PartnerListQueryUseCase.MAX_PAGE_SIZE)
            int size
    ) {
        return PartnerPageResponse.from(query.list(page, size));
    }
}
