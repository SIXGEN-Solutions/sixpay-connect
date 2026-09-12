package com.sixpay.accounting.application.exception;

import java.util.UUID;

public class AccountingT1OperationalCandidateNotFoundException
        extends RuntimeException {

    public AccountingT1OperationalCandidateNotFoundException(UUID candidateId) {
        super("Accounting T1 operational candidate not found: " + candidateId);
    }
}
