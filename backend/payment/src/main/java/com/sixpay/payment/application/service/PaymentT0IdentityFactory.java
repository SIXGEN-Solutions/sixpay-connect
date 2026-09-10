package com.sixpay.payment.application.service;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.model.evidence.PostingIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.PostingInstructionId;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Component
public final class PaymentT0IdentityFactory {

    private final IdentifierGenerator<UUID> identifierGenerator;

    public PaymentT0IdentityFactory(
            IdentifierGenerator<UUID> identifierGenerator
    ) {
        this.identifierGenerator =
                Objects.requireNonNull(identifierGenerator);
    }

    public PostingInstructionIdentity create(
            Payment payment,
            TreasuryAccountReference treasuryAccount
    ) {
        Objects.requireNonNull(payment, "Payment");
        Objects.requireNonNull(treasuryAccount, "Treasury account");

        var state = payment.toState();
        UUID instructionUuid = identifierGenerator.generate();

        return new PostingInstructionIdentity(
                new PostingInstructionId(instructionUuid),
                new PostingIdempotencyKey("t0:" + instructionUuid),
                state.requestedAmount(),
                state.debtorAccountReference().bindingFingerprint(),
                PaymentT0FinancialRequestFingerprint.from(
                        payment,
                        treasuryAccount
                )
        );
    }


}

/**
 * Canonical fingerprint of the immutable logical T0 financial request.
 */
final class PaymentT0FinancialRequestFingerprint {

    private PaymentT0FinancialRequestFingerprint() {
    }

    static EvidenceFingerprint from(
            Payment payment,
            TreasuryAccountReference treasuryAccount
    ) {
        Objects.requireNonNull(payment, "Payment");
        Objects.requireNonNull(treasuryAccount, "Treasury account");

        var state = payment.toState();

        return fromCanonical(
                state.publicPaymentReference().value(),
                state.requestedAmount().amount()
                        .stripTrailingZeros().toPlainString(),
                state.requestedAmount().currency().toString(),
                state.debtorAccountReference()
                        .integrationAccountToken(),
                treasuryAccount.accountToken()
        );
    }

    static EvidenceFingerprint from(
            PaymentFinancialEventSnapshot snapshot
    ) {
        Objects.requireNonNull(snapshot, "Financial snapshot");

        return fromCanonical(
                snapshot.publicPaymentReference().value(),
                snapshot.requestedAmount().amount()
                        .stripTrailingZeros().toPlainString(),
                snapshot.requestedAmount().currency().toString(),
                snapshot.debtorAccountReference(),
                snapshot.creditorAccountReference()
        );
    }

    private static EvidenceFingerprint fromCanonical(
            String paymentReference,
            String amount,
            String currency,
            String debtorAccountReference,
            String creditorAccountReference
    ) {
        String canonical =
                paymentReference
                        + "|" + amount
                        + "|" + currency
                        + "|" + debtorAccountReference
                        + "|" + creditorAccountReference;

        return EvidenceFingerprint.of(
                "v1:sha256:" + sha256(canonical)
        );
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}
