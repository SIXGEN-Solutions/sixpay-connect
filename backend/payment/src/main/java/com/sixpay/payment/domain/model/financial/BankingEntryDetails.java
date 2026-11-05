package com.sixpay.payment.domain.model.financial;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Optional Amplitude banking facts frozen at T0; unknown facts stay null. */
public record BankingEntryDetails(
        String age,
        String cha,
        String ncp,
        String suf,
        String clc,
        String ope,
        String mvti,
        String uti,
        String eve,
        LocalDate dco,
        LocalDate dva,
        String lib,
        String exo,
        String pie,
        String agsa,
        String agde,
        String devc,
        BigDecimal mctv,
        String nat,
        String eta
) {
}
