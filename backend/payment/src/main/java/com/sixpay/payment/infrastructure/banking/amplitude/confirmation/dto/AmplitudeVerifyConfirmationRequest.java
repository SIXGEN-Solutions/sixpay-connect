package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.dto;

public record AmplitudeVerifyConfirmationRequest(String paymentReference, String otp) { }
