package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

/** Bank confirmed that the configured Treasury account does not exist. */
public final class TreasuryAccountNotFoundException extends RuntimeException {
    public TreasuryAccountNotFoundException() {
        super("Configured Treasury account was not found by the bank");
    }
}
