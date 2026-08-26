package com.sixpay.accounting.application.service;

import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class AccountingBatchIdempotencyKeyFactory {

    public AccountingBatchIdempotencyKey create(
            String financialInstitutionCode,
            LocalDate businessDate,
            List<AccountingPaymentCandidate> candidates
    ) {
        if (financialInstitutionCode == null
                || financialInstitutionCode.isBlank()) {
            throw new IllegalArgumentException(
                    "financialInstitutionCode is required"
            );
        }

        Objects.requireNonNull(businessDate, "businessDate");
        Objects.requireNonNull(candidates, "candidates");

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "candidates must not be empty"
            );
        }

        String paymentIds = candidates.stream()
                .map(candidate ->
                        candidate.paymentId().toString()
                )
                .sorted()
                .reduce(
                        (left, right) ->
                                left + "," + right
                )
                .orElseThrow();

        String canonical =
                financialInstitutionCode.strip()
                        + "|"
                        + businessDate
                        + "|"
                        + paymentIds;

        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return new AccountingBatchIdempotencyKey(
                    HexFormat.of().formatHex(digest)
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot create accounting batch idempotency key",
                    exception
            );
        }
    }
}
