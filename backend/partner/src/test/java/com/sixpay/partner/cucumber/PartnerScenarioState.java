package com.sixpay.partner.cucumber;

import java.util.UUID;

public class PartnerScenarioState {
    public UUID partnerId;
    public UUID firstPartnerId;
    public int httpStatus;
    public String responseBody;
    public String idempotencyKey;
    public long partnerCountBefore;
    public long auditCountBefore;
    public long idempotencyCountBefore;
    public long outboxCountBefore;
    public long partnerCountAfter;
    public long auditCountAfter;
    public long idempotencyCountAfter;
    public long outboxCountAfter;
    public String statusBeforeFailure;
}
