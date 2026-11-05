package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.ClaimType;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentRequestIdentity;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguredTreasuryAccountResolutionAdapterTest {

    @Test
    void resolvesEachClaimFromTheConfiguredAccountWhenTheBankConfirmsExactIdentity() {
        for (ClaimType claim : ClaimType.values()) {
            String number = switch (claim) {
                case AVI -> "12345671051";
                case IM7 -> "23456781052";
                case RNF -> "34567891053";
            };
            var bank = mock(TreasuryAccountLookupClient.class);
            when(bank.lookup(eq("00001"), eq(number), any())).thenReturn(
                    new TreasuryAccountLookupResponse("SUCCESS", "ok",
                            new TreasuryAccountLookupResponse.TreasuryBankAccount(
                                    number, "00001", "001", "", "01", "1234567",
                                    "", "N", "N", null, BigDecimal.ZERO,
                                    "105", BigDecimal.ZERO, "", "ACTIVE")));
            TreasuryAccountReference resolved = new ConfiguredTreasuryAccountResolutionAdapter(
                    props(), bank).resolve(payment(claim), Instant.parse("2026-11-04T12:00:00Z"));
            assertEquals("00001-" + number + "-01", resolved.accountToken());
            verify(bank).lookup(eq("00001"), eq(number), any());
        }
    }

    @Test
    void rejectsAnAccountNotFoundEvenWhenHttpWasSuccessful() {
        var bank = mock(TreasuryAccountLookupClient.class);
        when(bank.lookup(eq("00001"), anyString(), any())).thenReturn(
                new TreasuryAccountLookupResponse("COMPTE_INEXISTANT", "not found", null));
        assertThrows(TreasuryAccountNotFoundException.class,
                () -> new ConfiguredTreasuryAccountResolutionAdapter(props(), bank)
                        .resolve(payment(ClaimType.AVI), Instant.now()));
    }

    @Test
    void rejectsAConflictingBankIdentity() {
        var bank = mock(TreasuryAccountLookupClient.class);
        when(bank.lookup(eq("00001"), anyString(), any())).thenReturn(
                new TreasuryAccountLookupResponse("SUCCESS", "ok",
                        new TreasuryAccountLookupResponse.TreasuryBankAccount(
                                "99999991051", "00001", "001", "", "01", "1234567",
                                "", "N", "N", null, BigDecimal.ZERO,
                                "105", BigDecimal.ZERO, "", "ACTIVE")));
        assertThrows(IllegalStateException.class,
                () -> new ConfiguredTreasuryAccountResolutionAdapter(props(), bank)
                        .resolve(payment(ClaimType.AVI), Instant.now()));
    }

    private static TreasuryResolutionProperties props() {
        return new TreasuryResolutionProperties(true, Map.of("BANK_CM", Map.of(
                "AVI", new TreasuryResolutionProperties.Account("A", "00001-12345671051-01", "v1"),
                "IM7", new TreasuryResolutionProperties.Account("B", "00001-23456781052-01", "v1"),
                "RNF", new TreasuryResolutionProperties.Account("C", "00001-34567891053-01", "v1")
        )));
    }

    private static Payment payment(ClaimType claim) {
        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        PaymentRequestIdentity identity = mock(PaymentRequestIdentity.class);
        when(payment.toState()).thenReturn(state);
        when(state.financialInstitutionCode()).thenReturn(FinancialInstitutionCode.of("BANK_CM"));
        when(state.initiationContext()).thenReturn(java.util.Optional.of(
                new PaymentInitiationContext(null, "PARTNER", null, claim, null, null,
                        com.sixpay.payment.domain.model.CallbackEndpoint.of("https://example.test/callback"))));
        when(state.requestIdentity()).thenReturn(identity);
        when(identity.correlationId()).thenReturn(CorrelationId.of("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
        return payment;
    }
}
