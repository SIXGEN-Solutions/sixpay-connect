package com.sixpay.bootstrap.integration.customer;

import com.sixpay.bootstrap.integration.customer.mapper.PaymentProjectionEventCommandMapper;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionRequest;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;

import java.util.Objects;

/**
 * Composition-layer adapter between Payment and Customer Observation.
 */
public final class ObservedCustomerProjectionModuleAdapter
        implements ObservedCustomerProjectionPort {

    private final ObserveCustomerUseCase observeCustomerUseCase;
    private final PaymentProjectionEventCommandMapper commandMapper;

    public ObservedCustomerProjectionModuleAdapter(
            ObserveCustomerUseCase observeCustomerUseCase
    ) {
        this(
                observeCustomerUseCase,
                new PaymentProjectionEventCommandMapper()
        );
    }

    public ObservedCustomerProjectionModuleAdapter(
            ObserveCustomerUseCase observeCustomerUseCase,
            PaymentProjectionEventCommandMapper commandMapper
    ) {
        this.observeCustomerUseCase = Objects.requireNonNull(
                observeCustomerUseCase,
                "observeCustomerUseCase is required"
        );
        this.commandMapper = Objects.requireNonNull(
                commandMapper,
                "commandMapper is required"
        );
    }

    @Override
    public ObservedCustomerProjectionResult project(
            ObservedCustomerProjectionRequest request
    ) {
        Objects.requireNonNull(request, "request is required");

        ObserveCustomerResult result = observeCustomerUseCase.observe(
                commandMapper.toCommand(request)
        );

        if (!request.sourceEventId().equals(result.sourceEventId())) {
            throw new IllegalStateException(
                    "Customer Observation returned a different sourceEventId"
            );
        }

        return new ObservedCustomerProjectionResult(
                result.sourceEventId(),
                result.observedCustomerId().value(),
                switch (result.disposition()) {
                    case APPLIED -> ObservedCustomerProjectionResult.Disposition.APPLIED;
                    case REPLAYED -> ObservedCustomerProjectionResult.Disposition.REPLAYED;
                    case IGNORED_STALE -> ObservedCustomerProjectionResult.Disposition.IGNORED_STALE;
                },
                result.projectionVersion()
        );
    }
}
