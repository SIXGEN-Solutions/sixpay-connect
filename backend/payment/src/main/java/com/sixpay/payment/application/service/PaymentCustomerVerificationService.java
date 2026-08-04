package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.CustomerVerificationEvidenceMapper;
import com.sixpay.payment.application.port.output.CustomerVerificationPort;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.application.port.output.PaymentCustomerVerificationIdGenerator;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Orchestrates the Payment-to-Customer verification workflow.
 *
 * <p>The workflow bean is available only when the intermodule
 * {@link CustomerVerificationPort} adapter is present. This keeps isolated
 * Payment tests and deployments valid while enabling the workflow in the full
 * SIXPAY bootstrap.</p>
 */
@Service
@ConditionalOnBean(CustomerVerificationPort.class)
public final class PaymentCustomerVerificationService {

    private final PaymentMutationCoordinator coordinator;
    private final CustomerVerificationPort customerVerificationPort;
    private final PaymentCustomerVerificationRequestFactory requestFactory;
    private final CustomerVerificationEvidenceMapper evidenceMapper;
    private final CustomerVerificationFailureMapper failureMapper;
    private final PaymentCustomerVerificationIdGenerator idGenerator;

    public PaymentCustomerVerificationService(
            PaymentMutationCoordinator coordinator,
            CustomerVerificationPort customerVerificationPort,
            PaymentCustomerVerificationRequestFactory requestFactory,
            CustomerVerificationEvidenceMapper evidenceMapper,
            CustomerVerificationFailureMapper failureMapper,
            PaymentCustomerVerificationIdGenerator idGenerator
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "coordinator is required"
        );
        this.customerVerificationPort = Objects.requireNonNull(
                customerVerificationPort,
                "customerVerificationPort is required"
        );
        this.requestFactory = Objects.requireNonNull(
                requestFactory,
                "requestFactory is required"
        );
        this.evidenceMapper = Objects.requireNonNull(
                evidenceMapper,
                "evidenceMapper is required"
        );
        this.failureMapper = Objects.requireNonNull(
                failureMapper,
                "failureMapper is required"
        );
        this.idGenerator = Objects.requireNonNull(
                idGenerator,
                "idGenerator is required"
        );
    }

    public PaymentWorkflowResult verifyCustomer(
            PaymentId paymentId,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(decisionAt, "decisionAt is required");
        Objects.requireNonNull(policies, "policies are required");

        return coordinator.mutate(
                paymentId,
                payment -> {
                    if (payment.status()
                            != PaymentStatus
                                    .BANKING_VERIFICATION_PENDING) {
                        throw new IllegalStateException(
                                "Customer verification requires "
                                        + "BANKING_VERIFICATION_PENDING, actual="
                                        + payment.status()
                        );
                    }

                    CustomerVerificationRequest request =
                            requestFactory.from(
                                    payment,
                                    idGenerator.nextId(),
                                    decisionAt
                            );

                    CustomerVerificationResponse response =
                            customerVerificationPort.verify(request);

                    var snapshot = evidenceMapper.toSnapshot(
                            response,
                            payment.toState()
                                    .requestIdentity()
                                    .correlationId(),
                            decisionAt
                    );

                    var failure = failureMapper.from(
                            response,
                            decisionAt
                    );

                    payment.recordBankingVerification(
                            snapshot,
                            failure,
                            decisionAt,
                            policies
                    );
                }
        );
    }
}
