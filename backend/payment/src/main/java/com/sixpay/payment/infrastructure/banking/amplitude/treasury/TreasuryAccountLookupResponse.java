package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import java.math.BigDecimal;

/** Transport DTO: bank-native codes and values are not interpreted by SIXPAY. */
public record TreasuryAccountLookupResponse(
        String code,
        String message,
        TreasuryBankAccount data
) {
    public record TreasuryBankAccount(
            String age,
            String ncp,
            String clc,
            String cli,
            String dev,
            String ife,
            String cfe,
            BigDecimal sin,
            String typ
    ) { }
}
