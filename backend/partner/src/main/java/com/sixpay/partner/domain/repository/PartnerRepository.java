package com.sixpay.partner.domain.repository;

import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerId;

import java.util.Optional;

public interface PartnerRepository {

    Partner save(Partner partner);

    Optional<Partner> findById(PartnerId partnerId);

    boolean existsById(PartnerId partnerId);
}
