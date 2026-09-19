package com.sixpay.payment.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.ClaimType;
import com.sixpay.payment.domain.model.ExternalSubscriptionReference;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.partner.application.contract.PartnerIdentity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitiatePaymentCommandTest {

    @Test
    void acceptsContractedTresorPayCommand() {
        InitiatePaymentCommand command =
                command(
                        new BigDecimal("600000"),
                        List.of(
                                beneficiary("300000"),
                                beneficiary("200000"),
                                beneficiary("100000")
                        )
                );

        assertThat(command.endToEndId())
                .isEqualTo("AVI-2025-00045678");
        assertThat(command.beneficiaries())
                .hasSize(3);
    }

    @Test
    void rejectsBeneficiarySumDifferentFromTotal() {
        assertThatThrownBy(() ->
                command(
                        new BigDecimal("600000"),
                        List.of(
                                beneficiary("300000"),
                                beneficiary("200000")
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "sum must equal total amount"
                );
    }

    private InitiatePaymentCommand command(
            BigDecimal totalAmount,
            List<PaymentBeneficiaryCommand> beneficiaries
    ) {
        return new InitiatePaymentCommand(
                PaymentSource.of("TRESOR_PAY"),
                ExternalSubscriptionReference.of("TRESOR_PAY:TP_APP_001"),
                PartnerIdentity.from("11111111-2222-3333-4444-555555555555"),
                "TP_APP_001",
                "AVI-2025-00045678",
                totalAmount,
                "XAF",
                "10005-00001-12345678901-12",
                "Société ABC SARL",
                ClaimType.AVI,
                "100200300",
                Instant.parse(
                        "2026-08-03T10:30:00Z"
                ),
                beneficiaries,
                "https://tresorpay.cm/callback",
                "idem-001",
                CorrelationId.of(
                        "11111111-1111-1111-1111-111111111111"
                )
        );
    }

    private PaymentBeneficiaryCommand beneficiary(
            String amount
    ) {
        return new PaymentBeneficiaryCommand(
                "10005-00001-000000TRESDGI-97",
                new BigDecimal(amount)
        );
    }
}
