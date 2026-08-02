package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.in.PaymentProjectionQueryUseCase;
import com.sixpay.payment.application.port.out.security.PaymentObjectAccessPort;
import com.sixpay.payment.application.port.out.security.PaymentProjectionReadPort;
import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.application.security.PaymentAccessDeniedException;
import com.sixpay.payment.application.security.PaymentAccessPolicy;
import com.sixpay.payment.application.security.PaymentAction;
import com.sixpay.payment.application.view.PaymentProjectionViews;
import com.sixpay.payment.domain.model.PaymentId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Security boundary for Payment query projections.
 */
@Service
@ConditionalOnBean({
        PaymentProjectionReadPort.class,
        PaymentObjectAccessPort.class,
        PaymentAccessPolicy.class
})
public class SecuredPaymentProjectionQueryService
        implements PaymentProjectionQueryUseCase {

    private final PaymentProjectionReadPort projectionReadPort;
    private final PaymentObjectAccessPort objectAccessPort;
    private final PaymentAccessPolicy accessPolicy;

    public SecuredPaymentProjectionQueryService(
            PaymentProjectionReadPort projectionReadPort,
            PaymentObjectAccessPort objectAccessPort,
            PaymentAccessPolicy accessPolicy
    ) {
        this.projectionReadPort = Objects.requireNonNull(
                projectionReadPort,
                "Payment projection read port"
        );
        this.objectAccessPort = Objects.requireNonNull(
                objectAccessPort,
                "Payment object access port"
        );
        this.accessPolicy = Objects.requireNonNull(
                accessPolicy,
                "Payment access policy"
        );
    }

    @Override
    public PaymentProjectionViews.SearchPage search(
            SearchPaymentProjectionsQuery query
    ) {
        Objects.requireNonNull(query, "Payment search query");

        return projectionReadPort.search(
                query,
                accessPolicy.requireSearchVisibility()
        );
    }

    @Override
    public Optional<PaymentProjectionViews.Detail> findById(
            UUID paymentId
    ) {
        PaymentId id = new PaymentId(
                Objects.requireNonNull(
                        paymentId,
                        "Payment ID"
                )
        );

        var descriptor = objectAccessPort
                .findAccessDescriptor(id)
                .orElseThrow(() ->
                        new PaymentAccessDeniedException(
                                "Payment is not visible"
                        )
                );

        accessPolicy.requireObjectAccess(
                PaymentAction.READ,
                descriptor
        );

        return projectionReadPort.findById(id);
    }
}
