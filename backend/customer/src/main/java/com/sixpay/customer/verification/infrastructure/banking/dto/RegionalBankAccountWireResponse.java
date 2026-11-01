package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Regional Core Banking V1 BankAccount transport schema.
 * This is a wire DTO, not SIXPAY's persisted customer account.
 * Values such as dev remain bank-native and are never guessed or ISO-converted.
 */
public record RegionalBankAccountWireResponse(
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
) {
    /** The canonical reference format is specified by Regional as age-ncp-clc. */
    public String accountReference() {
        if (blank(age) || blank(ncp) || blank(clc)) {
            throw new IllegalStateException("Regional account reference components are missing");
        }
        return age + "-" + ncp + "-" + clc;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
