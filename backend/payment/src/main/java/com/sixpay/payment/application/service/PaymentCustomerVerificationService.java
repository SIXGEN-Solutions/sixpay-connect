package com.sixpay.payment.application.service;

import com.sixpay.payment.application.exception.PaymentCustomerVerificationRetryableException;
import com.sixpay.payment.application.port.output.CustomerVerificationEvidenceMapper;
import com.sixpay.payment.application.port.output.CustomerVerificationPort;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.application.port.output.CustomerVerificationTechnicalException;
import com.sixpay.payment.application.port.output.PaymentCustomerVerificationIdGenerator;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates the Payment-to-Customer verification workflow.
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

        UUID verificationId = idGenerator.forPayment(paymentId);

        return coordinator.mutate(
                paymentId,
                payment -> {
                    if (payment.status()
                            != PaymentStatus
                                    .BANKING_VERIFICATION_PENDING) {
                        /*
                         * A relayed integration message may be replayed when the
                         * business mutation succeeded but publication
                         * acknowledgement failed. Once banking evidence is
                         * already durable, the replay has no additional effect.
                         */
                        if (payment.toState()
                                .bankingVerificationEvidence()
                                .isPresent()) {
                            return;
                        }

                        throw new IllegalStateException(
                                "Customer verification requires "
                                        + "BANKING_VERIFICATION_PENDING, actual="
                                        + payment.status()
                        );
                    }

                    CustomerVerificationRequest request =
                            requestFactory.from(
                                    payment,
                                    verificationId,
                                    decisionAt
                            );

                    CustomerVerificationResponse response;
                    try {
                        response =
                                customerVerificationPort.verify(request);
                    } catch (CustomerVerificationTechnicalException failure) {
                        /*
                         * No aggregate mutation has occurred. The coordinator
                         * therefore persists neither Payment nor new events.
                         * The already durable
                         * PaymentBankingVerificationRequested message can be
                         * replayed with the same paymentId, correlationId,
                         * binding fingerprint and verificationId.
                         */
                        throw new PaymentCustomerVerificationRetryableException(
                                paymentId,
                                failure.verificationId(),
                                failure.errorType(),
                                failure
                        );
                    }

                    /*
                     * Customer timestamps are canonical and stable across a
                     * replay. Using completedAt rather than the current retry
                     * time makes the resulting evidence and failure identical.
                     */
                    var snapshot = evidenceMapper.toSnapshot(
                            response,
                            payment.toState()
                                    .requestIdentity()
                                    .correlationId(),
                            response.completedAt()
                    );

                    var failure = failureMapper.from(
                            response,
                            response.completedAt()
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
