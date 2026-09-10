package com.sixpay.accounting.application.service;

import com.sixpay.accounting.domain.model.TfjConfirmation;

import java.util.Objects;

public record TfjFinalityReadyEvent(TfjConfirmation confirmation) {
    public TfjFinalityReadyEvent {
        confirmation = Objects.requireNonNull(confirmation, "confirmation");
    }
}
