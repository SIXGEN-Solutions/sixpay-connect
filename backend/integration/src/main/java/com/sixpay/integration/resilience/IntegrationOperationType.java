package com.sixpay.integration.resilience;
public enum IntegrationOperationType {
    READ_ONLY, IDEMPOTENT_COMMAND, FINANCIAL_COMMAND, CALLBACK, MESSAGE_CONSUMPTION
}
