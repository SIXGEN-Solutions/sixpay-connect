package com.sixpay.payment.application.service;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
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

        String canonical =
                state.publicPaymentReference().value()
                        + "|" + state.requestedAmount().amount()
                                .stripTrailingZeros().toPlainString()
                        + "|" + state.requestedAmount().currency()
                        + "|" + state.debtorAccountReference().integrationAccountToken()
                        + "|" + treasuryAccount.accountToken();

        return new PostingInstructionIdentity(
                new PostingInstructionId(instructionUuid),
                new PostingIdempotencyKey("t0:" + instructionUuid),
                state.requestedAmount(),
                state.debtorAccountReference().bindingFingerprint(),
                EvidenceFingerprint.of(
                        "v1:sha256:" + sha256(canonical)
                )
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
