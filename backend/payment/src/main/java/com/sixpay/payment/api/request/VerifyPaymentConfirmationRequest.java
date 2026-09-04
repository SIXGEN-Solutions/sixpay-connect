package com.sixpay.payment.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public final class VerifyPaymentConfirmationRequest {

    private final char[] otp;

    public VerifyPaymentConfirmationRequest(
            @JsonProperty(value = "otp", access = JsonProperty.Access.WRITE_ONLY)
            char[] otp
    ) {
        if (otp == null || otp.length == 0) {
            throw new IllegalArgumentException("OTP must not be empty");
        }
        this.otp = Arrays.copyOf(otp, otp.length);
    }

    @JsonProperty(value = "otp", access = JsonProperty.Access.WRITE_ONLY)
    public char[] otp() {
        return Arrays.copyOf(otp, otp.length);
    }

    @Override
    public String toString() {
        return "VerifyPaymentConfirmationRequest[otp=<redacted>]";
    }
}
