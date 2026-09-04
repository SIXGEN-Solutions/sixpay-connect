package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.api.request.InitiateDebitBeneficiaryRequest;
import com.sixpay.payment.api.request.InitiateDebitRequest;
import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.ClaimType;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCommandApiMapperTest {

    private final PaymentCommandApiMapper mapper =
            new PaymentCommandApiMapper();

    @Test
    void mapsContractRequestWithoutAuthenticationSecrets() {
        var request = request();

        var command = mapper.toCommand(
                request,
                "TRESOR_PAY",
                "IDEMPOTENCY-001",
                CorrelationId.of(
                        "11111111-1111-1111-1111-111111111111"
                )
        );

        assertThat(command.partnerLoginName())
                .isEqualTo("TRESOR_PAY");
        assertThat(command.authenticatedPartnerLoginName())
                .isEqualTo("TRESOR_PAY");
        assertThat(command.beneficiaries())
                .hasSize(1);
    }

    @Test
    void omitsUnavailableBankChallengeData() {
        InitiateDebitResult result =
                InitiateDebitResult.accepted(
                        new PaymentId(UUID.randomUUID()),
                        PublicPaymentReference.of(
                                "PAY-1234567890ABCDEFGHJKMNPQRS"
                        ),
                        "AVI-2025-00045678",
                        Money.of(
                                new BigDecimal("600000"),
                                "XAF"
                        ),
                        Instant.parse(
                                "2026-08-03T10:30:00Z"
                        )
                );

        var response = mapper.toResponse(result);

        assertThat(response.status().name())
                .isEqualTo("RECEIVED");
        assertThat(response.bankOperationId()).isNull();
        assertThat(response.fees()).isNull();
        assertThat(response.transactionQrCode()).isNull();
    }

    private static InitiateDebitRequest request() {
        return new InitiateDebitRequest(
                "TRESOR_PAY",
                "TP_APP_001",
                "AVI-2025-00045678",
                new BigDecimal("600000"),
                "XAF",
                "10005-00001-12345678901-12",
                "Société ABC SARL",
                ClaimType.AVI,
                "100200300",
                Instant.parse(
                        "2026-08-03T10:30:00Z"
                ),
                List.of(
                        new InitiateDebitBeneficiaryRequest(
                                "10005-00001-TRESDGI-97",
                                new BigDecimal("600000")
                        )
                ),
                "https://tresorpay.cm/callback"
        );
    }
}
