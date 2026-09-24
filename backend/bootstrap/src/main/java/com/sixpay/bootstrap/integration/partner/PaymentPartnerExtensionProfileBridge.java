package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.contract.PartnerExtensionProfileQuery;
import com.sixpay.partner.application.contract.PartnerIdentity;
import com.sixpay.payment.application.port.output.partner.PartnerExtensionDefinition;
import com.sixpay.payment.application.port.output.partner.PartnerExtensionProfile;
import com.sixpay.payment.application.port.output.partner.PartnerExtensionProfilePort;
import com.sixpay.payment.application.port.output.partner.PartnerExtensionValueType;
import com.sixpay.payment.domain.model.CanonicalPartnerIdentity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public final class PaymentPartnerExtensionProfileBridge
        implements PartnerExtensionProfilePort {

    private final PartnerExtensionProfileQuery partnerQuery;

    public PaymentPartnerExtensionProfileBridge(
            PartnerExtensionProfileQuery partnerQuery
    ) {
        this.partnerQuery = Objects.requireNonNull(partnerQuery);
    }

    @Override
    public PartnerExtensionProfile resolve(CanonicalPartnerIdentity partnerIdentity) {
        var view = partnerQuery.resolve(
                new PartnerIdentity(
                        Objects.requireNonNull(partnerIdentity).value()
                )
        );

        return new PartnerExtensionProfile(
                view.definitions().stream()
                        .map(item -> new PartnerExtensionDefinition(
                                item.key(),
                                PartnerExtensionValueType.valueOf(item.valueType()),
                                item.idempotencySignificant(),
                                item.queryExposable(),
                                item.callbackExposable(),
                                item.securityClassification()
                        ))
                        .collect(Collectors.toUnmodifiableMap(
                                PartnerExtensionDefinition::key,
                                Function.identity()
                        ))
        );
    }
}
