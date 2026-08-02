package com.sixpay.payment.infrastructure.initiation;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.application.command.InitiateDebitBeneficiaryCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.domain.model.ClaimType;
import com.sixpay.payment.domain.model.PaymentSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentInitiationPreparationAdapterTest {

    private static final Instant NOW =
            Instant.parse("2026-08-03T10:30:00Z");

    private static final UUID PAYMENT_UUID =
            UUID.fromString(
                    "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
            );

    private final IdentifierGenerator<UUID> identifiers =
            () -> PAYMENT_UUID;

    private final PaymentInitiationPreparationAdapter adapter =
            new PaymentInitiationPreparationAdapter(
                    identifiers
            );

    @Test
    void preparesProtectedDomainIntentWithoutClearRib() {
        InitiateDebitCommand command = command(
                List.of(
                        beneficiary(
                                "10005-00001-TRESDGI-97",
                                "300000"
                        ),
                        beneficiary(
                                "10005-00001-TRESDOUANE-11",
                                "300000"
                        )
                )
        );

        var prepared = adapter.prepare(
                command,
                "a".repeat(64),
                NOW
        );

        assertThat(prepared.paymentId().value())
                .isEqualTo(PAYMENT_UUID);
        assertThat(prepared.publicPaymentReference().value())
                .startsWith("PAY-");
        assertThat(prepared.receivedAt()).isEqualTo(NOW);

        var intent = prepared.intent();

        assertThat(intent.source())
                .isEqualTo(PaymentSource.TRESOR_PAY);
        assertThat(intent.externalPaymentReference().value())
                .isEqualTo("AVI-2025-00045678");
        assertThat(intent.requestIdentity()
                .requestFingerprint().value())
                .isEqualTo("a".repeat(64));

        var debtor = intent.debtorAccountReference();

        assertThat(debtor.financialInstitutionCode().value())
                .isEqualTo("10005");
        assertThat(debtor.maskedDisplay())
                .isEqualTo("RIB-****-0112");
        assertThat(debtor.integrationAccountToken())
                .startsWith("acct:v1:")
                .doesNotContain(command.debtorRib());
        assertThat(debtor.bindingFingerprint())
                .matches("^v1:[0-9a-f]{64}$");

        assertThat(intent.treasuryAllocationIntent()
                .allocations())
                .hasSize(2);
        assertThat(intent.initiationContext())
                .isNotNull();
        assertThat(intent.initiationContext()
                .callbackEndpoint().value())
                .isEqualTo(
                        "https://tresorpay.cm/callback"
                );
    }

    @Test
    void producesStableAllocationFingerprintRegardlessOfOrder() {
        var first = beneficiary(
                "10005-00001-TRESDGI-97",
                "300000"
        );
        var second = beneficiary(
                "10005-00001-TRESDOUANE-11",
                "300000"
        );

        var preparedOne = adapter.prepare(
                command(List.of(first, second)),
                "b".repeat(64),
                NOW
        );

        var preparedTwo = adapter.prepare(
                command(List.of(second, first)),
                "b".repeat(64),
                NOW
        );

        assertThat(
                preparedOne.intent()
                        .allocationIntentFingerprint()
        ).isEqualTo(
                preparedTwo.intent()
                        .allocationIntentFingerprint()
        );
    }

    @Test
    void rejectsMalformedRequestHash() {
        assertThatThrownBy(() ->
                adapter.prepare(
                        command(
                                List.of(
                                        beneficiary(
                                                "10005-00001-TRESDGI-97",
                                                "600000"
                                        )
                                )
                        ),
                        "not-a-sha256",
                        NOW
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "64 lowercase hexadecimal"
                );
    }


    private static InitiateDebitCommand command(
            List<InitiateDebitBeneficiaryCommand> beneficiaries
    ) {
        BigDecimal total = beneficiaries.stream()
                .map(InitiateDebitBeneficiaryCommand::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InitiateDebitCommand(
                "TRESOR_PAY",
                "TRESOR_PAY",
                "TP_APP_001",
                "AVI-2025-00045678",
                total,
                "XAF",
                "10005-00001-12345678901-12",
                "Société ABC SARL",
                ClaimType.AVI,
                "100200300",
                NOW,
                beneficiaries,
                "https://tresorpay.cm/callback",
                "IDEMPOTENCY-00000001",
                CorrelationId.of(
                        "11111111-1111-1111-1111-111111111111"
                )
        );
    }

    private static InitiateDebitBeneficiaryCommand beneficiary(
            String rib,
            String amount
    ) {
        return new InitiateDebitBeneficiaryCommand(
                rib,
                new BigDecimal(amount)
        );
    }

}
