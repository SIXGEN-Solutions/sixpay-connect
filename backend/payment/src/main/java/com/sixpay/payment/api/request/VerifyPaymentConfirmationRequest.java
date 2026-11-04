package com.sixpay.payment.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;

public final class VerifyPaymentConfirmationRequest {

    @Schema(type = "string", minLength = 1, accessMode = Schema.AccessMode.WRITE_ONLY,
            description = "Transient OTP; never persist or log")
    private final char[] otp;

    /** JSON boundary is an OpenAPI string; internal state remains defensively copied char[]. */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static VerifyPaymentConfirmationRequest fromJson(
            @JsonProperty(value = "otp", access = JsonProperty.Access.WRITE_ONLY)
            String otp
    ) {
        if (otp == null || otp.isEmpty()) {
            throw new IllegalArgumentException("OTP must not be empty");
        }
        return new VerifyPaymentConfirmationRequest(otp.toCharArray());
    }

    public VerifyPaymentConfirmationRequest(char[] otp) {
        if (otp == null || otp.length == 0) {
            throw new IllegalArgumentException("OTP must not be empty");
        }
        this.otp = Arrays.copyOf(otp, otp.length);
    }

    @JsonProperty(value = "otp", access = JsonProperty.Access.WRITE_ONLY)
    @Schema(type = "string", minLength = 1, accessMode = Schema.AccessMode.WRITE_ONLY)
    public char[] otp() {
        return Arrays.copyOf(otp, otp.length);
    }

    @Override
    public String toString() {
        return "VerifyPaymentConfirmationRequest[otp=<redacted>]";
    }
}
