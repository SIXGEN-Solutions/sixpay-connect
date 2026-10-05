package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.domain.model.DebtorAccountReference;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Builds the Payment canonical debtor-account reference only from
 * authoritative VERIFIED banking data.
 */
@Component
public final class ResolvedDebtorAccountReferenceFactory {

    public DebtorAccountReference from(
            CustomerVerificationResponse response
    ) {
        Objects.requireNonNull(response, "response is required");

        if (response.outcome()
                != CustomerVerificationResponse.Outcome.VERIFIED) {
            throw new IllegalArgumentException(
                    "Canonical debtor account requires VERIFIED banking result"
            );
        }

        String accountReference = requireText(
                response.accountReference(),
                "accountReference"
        );
        String customerReference = requireText(
                response.customerReference(),
                "customerReference"
        );
        String institution = requireText(
                response.accountFinancialInstitutionCode(),
                "accountFinancialInstitutionCode"
        );
        String masked = requireText(
                response.maskedAccountIdentifier(),
                "maskedAccountIdentifier"
        );

        return new DebtorAccountReference(
                FinancialInstitutionCode.of(institution),
                accountReference,
                masked,
                fingerprint(institution, customerReference, accountReference)
        );
    }

    private static String fingerprint(
            String institution,
            String customerReference,
            String accountReference
    ) {
        String canonical = institution
                + "|" + customerReference
                + "|" + accountReference;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "v1:" + HexFormat.of().formatHex(
                    digest.digest(canonical.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
