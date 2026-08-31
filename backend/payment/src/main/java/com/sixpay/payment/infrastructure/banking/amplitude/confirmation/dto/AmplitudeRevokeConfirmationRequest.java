package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.dto;

public record AmplitudeRevokeConfirmationRequest(String paymentReference, String reasonCode) { }
