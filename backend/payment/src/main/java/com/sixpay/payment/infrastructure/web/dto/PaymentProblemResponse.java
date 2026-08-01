package com.sixpay.payment.infrastructure.web.dto;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public record PaymentProblemResponse(URI type, String title, int status, String code, UUID correlationId, String detail, Integer retryAfter, List<Violation> violations) {
    public PaymentProblemResponse {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }
    public record Violation(String field, String code, String message) {}
}
