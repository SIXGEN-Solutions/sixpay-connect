package com.sixpay.accounting.application.service;

import com.sixpay.accounting.domain.model.TfjConfirmation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class TfjLogicalFingerprint {

    private TfjLogicalFingerprint() {
    }

    public static String sha256(TfjConfirmation confirmation) {
        String canonical = String.join(
                "|",
                confirmation.confirmationId().toString(),
                confirmation.financialInstitutionCode(),
                confirmation.businessDate().toString(),
                confirmation.paymentReference(),
                confirmation.bankPostingReference(),
                nullToEmpty(confirmation.tfjBatchReference()),
                confirmation.status().name(),
                confirmation.confirmedAt().toString(),
                nullToEmpty(confirmation.failureCode()),
                nullToEmpty(confirmation.failureDescription()),
                confirmation.recoveryAction() == null
                        ? ""
                        : confirmation.recoveryAction().name()
        );

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
