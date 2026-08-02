package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.command.InitiateDebitBeneficiaryCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.domain.model.ClaimType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentInitiationCanonicalizerTest {

    private static final Instant NOW =
            Instant.parse("2026-08-03T10:30:00Z");

    private final PaymentInitiationCanonicalizer canonicalizer =
            new PaymentInitiationCanonicalizer();

    @Test
    void beneficiaryOrderDoesNotChangeCanonicalRequest() {
        var first = beneficiary(
                "10005-00001-TRESDGI-97",
                "300000"
        );
        var second = beneficiary(
                "10005-00001-TRESDOUANE-11",
                "300000"
        );

        assertThat(
                canonicalizer.canonicalize(
                        command(List.of(first, second))
                )
        ).isEqualTo(
                canonicalizer.canonicalize(
                        command(List.of(second, first))
                )
        );
    }

    @Test
    void materialBusinessChangeChangesCanonicalRequest() {
        InitiateDebitCommand original =
                command(
                        List.of(
                                beneficiary(
                                        "10005-00001-TRESDGI-97",
                                        "600000"
                                )
                        )
                );

        InitiateDebitCommand changed =
                new InitiateDebitCommand(
                        original.partnerLoginName(),
                        original.authenticatedPartnerLoginName(),
                        original.applicationId(),
                        "AVI-2025-00099999",
                        original.totalAmount(),
                        original.currency(),
                        original.debtorRib(),
                        original.debtorName(),
                        original.claimType(),
                        original.taxpayerIdentifier(),
                        original.requestedExecutionAt(),
                        original.beneficiaries(),
                        original.callbackUrl(),
                        original.idempotencyKey(),
                        original.correlationId()
                );

        assertThat(canonicalizer.canonicalize(changed))
                .isNotEqualTo(
                        canonicalizer.canonicalize(original)
                );
    }

    @Test
    void canonicalRequestNeverContainsIdempotencyKey() {
        InitiateDebitCommand command =
                command(
                        List.of(
                                beneficiary(
                                        "10005-00001-TRESDGI-97",
                                        "600000"
                                )
                        )
                );

        assertThat(canonicalizer.canonicalize(command))
                .doesNotContain(command.idempotencyKey());
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
