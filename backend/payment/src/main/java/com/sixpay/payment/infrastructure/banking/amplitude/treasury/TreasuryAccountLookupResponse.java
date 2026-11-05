package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Transport DTO for Regional V1 AccountLookupResult (not SIXPAY normalized BankAccount). */
public record TreasuryAccountLookupResponse(
        String code,
        String message,
        TreasuryBankAccount data
) {
    /** Complete native BKCOM account response; no extra business checks added by SIXPAY. */
    public record TreasuryBankAccount(
            String ncp,
            String age,
            String dev,
            String cha,
            String clc,
            String cli,
            String inti,
            String ife,
            String cfe,
            LocalDate dou,
            BigDecimal sin,
            String typ,
            BigDecimal sde,
            String suf,
            String status
    ) { }
}
