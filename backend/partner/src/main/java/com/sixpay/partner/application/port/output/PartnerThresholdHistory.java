package com.sixpay.partner.application.port.output;

@FunctionalInterface
public interface PartnerThresholdHistory {

    void append(PartnerThresholdHistoryRecord record);
}
