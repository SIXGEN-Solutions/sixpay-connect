package com.sixpay.customer.observation.application.service;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import com.sixpay.customer.observation.application.port.output.ObservedCustomerIdGenerator;
import com.sixpay.customer.observation.application.port.output.ObservedCustomerRepository;
import com.sixpay.customer.observation.application.port.output.ObservedPaymentRepository;
import com.sixpay.customer.observation.domain.model.ObservationApplicationResult;
import com.sixpay.customer.observation.domain.model.ObservedCustomer;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedCustomerObservation;

import java.util.Objects;
import java.util.Optional;

/**
 * Framework-free orchestration service for the Observed Customer projection.
 *
 * <p>The service owns no Payment, HTTP, Amplitude, JPA or event-consumer type.
 * Technical transaction demarcation belongs to the persistence assembly.</p>
 */
public final class ObservedCustomerProjectionService
        implements ObserveCustomerUseCase {

    private final ObservedCustomerRepository customerRepository;
    private final ObservedPaymentRepository paymentRepository;
    private final ObservedCustomerIdGenerator idGenerator;

    public ObservedCustomerProjectionService(
            ObservedCustomerRepository customerRepository,
            ObservedPaymentRepository paymentRepository,
            ObservedCustomerIdGenerator idGenerator
    ) {
        this.customerRepository = Objects.requireNonNull(
                customerRepository,
                "customerRepository is required"
        );
        this.paymentRepository = Objects.requireNonNull(
                paymentRepository,
                "paymentRepository is required"
        );
        this.idGenerator = Objects.requireNonNull(
                idGenerator,
                "idGenerator is required"
        );
    }

    @Override
    public ObserveCustomerResult observe(
            ObserveCustomerCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        Optional<ObservedCustomer> existing =
                customerRepository.findByNormalizedNiu(
                        command.normalizedNiu()
                );

        ObservedCustomerObservation observation =
                toObservation(command);

        if (existing.isEmpty()) {
            return createProjection(command, observation);
        }

        return updateProjection(
                command,
                existing.orElseThrow(),
                observation
        );
    }

    private ObserveCustomerResult createProjection(
            ObserveCustomerCommand command,
            ObservedCustomerObservation observation
    ) {
        ObservedCustomerId observedCustomerId =
                Objects.requireNonNull(
                        idGenerator.nextId(),
                        "idGenerator returned null"
                );

        ObservedCustomer customer =
                ObservedCustomer.observeFirst(
                        observedCustomerId,
                        observation
                );

        persistAppliedObservation(
                customer,
                command
        );

        return result(
                customer,
                command,
                ObserveCustomerResult.Disposition.APPLIED
        );
    }

    private ObserveCustomerResult updateProjection(
            ObserveCustomerCommand command,
            ObservedCustomer customer,
            ObservedCustomerObservation observation
    ) {
        ObservationApplicationResult applicationResult =
                customer.observePayment(observation);

        if (applicationResult
                == ObservationApplicationResult.REPLAYED) {
            return result(
                    customer,
                    command,
                    ObserveCustomerResult.Disposition.REPLAYED
            );
        }

        persistAppliedObservation(
                customer,
                command
        );

        return result(
                customer,
                command,
                toDisposition(applicationResult)
        );
    }

    private void persistAppliedObservation(
            ObservedCustomer customer,
            ObserveCustomerCommand command
    ) {
        /*
         * Lot 4.5.5 must implement these two Customer-owned ports within one
         * transaction. The application service intentionally remains
         * framework-free.
         */
        customerRepository.save(customer);

        paymentRepository.save(
                customer.id(),
                command.sourceEventId(),
                command.payment(),
                command.watermark(),
                command.observedAt()
        );
    }

    private static ObservedCustomerObservation toObservation(
            ObserveCustomerCommand command
    ) {
        return new ObservedCustomerObservation(
                command.sourceEventId(),
                command.identity(),
                command.institution(),
                command.payment(),
                command.watermark(),
                command.observedAt(),
                command.observedAt()
        );
    }

    private static ObserveCustomerResult result(
            ObservedCustomer customer,
            ObserveCustomerCommand command,
            ObserveCustomerResult.Disposition disposition
    ) {
        return new ObserveCustomerResult(
                customer.id(),
                command.sourceEventId(),
                command.paymentId(),
                disposition,
                customer.projectionVersion(),
                command.observedAt()
        );
    }

    private static ObserveCustomerResult.Disposition toDisposition(
            ObservationApplicationResult applicationResult
    ) {
        return switch (applicationResult) {
            case APPLIED_NEW_PAYMENT,
                 APPLIED_PAYMENT_UPDATE ->
                    ObserveCustomerResult.Disposition.APPLIED;
            case APPLIED_STALE_HISTORY ->
                    ObserveCustomerResult.Disposition.IGNORED_STALE;
            case REPLAYED ->
                    ObserveCustomerResult.Disposition.REPLAYED;
        };
    }
}
