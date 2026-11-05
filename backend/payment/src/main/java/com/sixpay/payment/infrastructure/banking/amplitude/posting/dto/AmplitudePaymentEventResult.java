package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Regional PaymentEventResult; dco consumer extension requires Regional publication. */
public class AmplitudePaymentEventResult {
    private String paymentReference;
    private String outcome;
    private List<AmplitudePaymentExecutionCheck> checks;
    private String bankReference;
    private String reasonCode;
    private Instant observedAt;
    private LocalDate dco;

    public AmplitudePaymentEventResult() { }

    public AmplitudePaymentEventResult(String paymentReference, String outcome,
            List<AmplitudePaymentExecutionCheck> checks, String bankReference,
            String reasonCode, Instant observedAt) {
        this(paymentReference, outcome, checks, bankReference, reasonCode, observedAt, null);
    }

    public AmplitudePaymentEventResult(String paymentReference, String outcome,
            List<AmplitudePaymentExecutionCheck> checks, String bankReference,
            String reasonCode, Instant observedAt, LocalDate dco) {
        this.paymentReference = paymentReference;
        this.outcome = outcome;
        this.checks = checks;
        this.bankReference = bankReference;
        this.reasonCode = reasonCode;
        this.observedAt = observedAt;
        this.dco = dco;
    }

    public String paymentReference() { return paymentReference; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String value) { paymentReference = value; }
    public String outcome() { return outcome; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String value) { outcome = value; }
    public List<AmplitudePaymentExecutionCheck> checks() { return checks; }
    public List<AmplitudePaymentExecutionCheck> getChecks() { return checks; }
    public void setChecks(List<AmplitudePaymentExecutionCheck> value) { checks = value; }
    public String bankReference() { return bankReference; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String value) { bankReference = value; }
    public String reasonCode() { return reasonCode; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { reasonCode = value; }
    public Instant observedAt() { return observedAt; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant value) { observedAt = value; }
    public LocalDate dco() { return dco; }
    public LocalDate getDco() { return dco; }
    public void setDco(LocalDate value) { dco = value; }
    public LocalDate accountingDate() { return dco; }
}
