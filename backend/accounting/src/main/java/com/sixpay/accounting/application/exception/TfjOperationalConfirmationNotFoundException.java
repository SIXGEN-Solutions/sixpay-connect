package com.sixpay.accounting.application.exception;

import java.util.UUID;

public class TfjOperationalConfirmationNotFoundException extends RuntimeException {

    public TfjOperationalConfirmationNotFoundException(UUID confirmationId) {
        super("TFJ operational confirmation not found: " + confirmationId);
    }
}
