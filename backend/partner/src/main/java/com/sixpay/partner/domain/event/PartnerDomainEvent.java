package com.sixpay.partner.domain.event;

import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.sharedkernel.domain.event.DomainEvent;

public sealed interface PartnerDomainEvent
        extends DomainEvent
        permits PartnerCreated, PartnerStatusChanged, PartnerThresholdConfigured {

    PartnerId partnerId();
}
