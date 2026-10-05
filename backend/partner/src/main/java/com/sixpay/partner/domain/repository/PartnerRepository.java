package com.sixpay.partner.domain.repository;

import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerIdentifier;

import java.util.Optional;

public interface PartnerRepository {

    Partner save(Partner partner);

    Optional<Partner> findById(PartnerId partnerId);

    Optional<Partner> findByPartnerIdentifier(PartnerIdentifier partnerIdentifier);

    boolean existsById(PartnerId partnerId);
}
