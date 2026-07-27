package com.sixpay.partner.application.port.in;

import com.sixpay.partner.application.view.PartnerAuditPage;
import com.sixpay.partner.application.view.PartnerView;
import com.sixpay.partner.domain.model.PartnerId;

import java.time.Instant;
public interface PartnerQueryUseCase {

    PartnerView findById(PartnerId partnerId);

    PartnerAuditPage findAuditTrail(PartnerId partnerId, Instant from, Instant to, int page, int size);
}
