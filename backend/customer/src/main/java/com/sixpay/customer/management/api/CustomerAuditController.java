package com.sixpay.customer.management.api;

import com.sixpay.customer.management.api.response.CustomerAuditRecordResponse;
import com.sixpay.customer.management.application.port.output.CustomerAuditTrail;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/customer-audit-records")
public class CustomerAuditController {

    private final CustomerAuditTrail auditTrail;

    public CustomerAuditController(
            CustomerAuditTrail auditTrail
    ) {
        this.auditTrail = auditTrail;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_customer.audit.read')")
    public List<CustomerAuditRecordResponse> find(
            @RequestParam String aggregateType,
            @RequestParam UUID aggregateId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "from must be before or equal to to"
            );
        }

        return auditTrail.find(
                        aggregateType,
                        aggregateId,
                        from,
                        to
                )
                .stream()
                .map(CustomerAuditRecordResponse::from)
                .toList();
    }
}
