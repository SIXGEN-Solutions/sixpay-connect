package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.management.application.port.output.PartnerSubscriptionEligibilityPort;
import com.sixpay.partner.application.exception.PartnerNotFoundException;
import com.sixpay.partner.application.port.in.PartnerQueryUseCase;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public final class PartnerSubscriptionEligibilityAdapter
        implements PartnerSubscriptionEligibilityPort {

    private final PartnerQueryUseCase partnerQuery;

    public PartnerSubscriptionEligibilityAdapter(
            PartnerQueryUseCase partnerQuery
    ) {
        this.partnerQuery =
                Objects.requireNonNull(partnerQuery);
    }

    @Override
    public PartnerEligibility check(UUID partnerId) {
        try {
            var partner = partnerQuery.findById(
                    new PartnerId(partnerId)
            );

            return new PartnerEligibility(
                    true,
                    partner.status()
                            == PartnerStatus.ACTIVE
            );
        } catch (PartnerNotFoundException exception) {
            return new PartnerEligibility(
                    false,
                    false
            );
        }
    }
}
