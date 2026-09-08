package com.sixpay.accounting.application.service;

import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class AccountingBatchIdempotencyKeyFactory {
    public AccountingBatchIdempotencyKey create(
            String institution, LocalDate businessDate, List<AccountingPaymentCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        return createCanonical(
                institution,
                businessDate,
                candidates.stream()
                        .map(candidate -> candidate.paymentId() + ":" + candidate.financialSnapshotId())
                        .toList()
        );
    }

    public AccountingBatchIdempotencyKey createFromProjections(
            String institution, LocalDate businessDate, List<AccountingCandidateProjection> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        return createCanonical(institution, businessDate,
                candidates.stream().map(c -> c.paymentId() + ":" + c.financialSnapshotId()).toList());
    }

    private AccountingBatchIdempotencyKey createCanonical(
            String institution, LocalDate businessDate, List<String> identities) {
        if (institution == null || institution.isBlank()) {
            throw new IllegalArgumentException("financialInstitutionCode is required");
        }
        Objects.requireNonNull(businessDate, "businessDate");
        if (identities == null || identities.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        String canonicalIds = identities.stream().sorted()
                .reduce((a, b) -> a + "," + b).orElseThrow();
        String canonical = institution.strip() + "|" + businessDate + "|" + canonicalIds;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return new AccountingBatchIdempotencyKey(HexFormat.of().formatHex(digest));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create accounting batch idempotency key", e);
        }
    }
}
