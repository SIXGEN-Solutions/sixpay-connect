package com.sixpay.payment.application.service;

import com.sixpay.payment.application.exception.PaymentCustomerVerificationRetryableException;
import com.sixpay.payment.application.exception.PaymentConcurrentMutationException;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import java.util.concurrent.locks.ReentrantLock;
import com.sixpay.payment.application.port.output.CustomerVerificationEvidenceMapper;
import com.sixpay.payment.application.port.output.CustomerVerificationPort;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.application.port.output.CustomerVerificationTechnicalException;
import com.sixpay.payment.application.port.output.PaymentCustomerVerificationIdGenerator;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.apache.juli.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final ResolvedDebtorAccountReferenceFactory accountReferenceFactory;

    // Fixed-size in-JVM stripes coordinate concurrent payment verification
    // without holding any database transaction during Core Banking I/O.
    // Distributed races are still handled by optimistic-write reconciliation.
    private static final ReentrantLock[] VERIFICATION_GATES = createVerificationGates();

    private static ReentrantLock[] createVerificationGates() {
        ReentrantLock[] gates = new ReentrantLock[1024];
        for (int i = 0; i < gates.length; i++) {
            gates[i] = new ReentrantLock();
        }
        return gates;
    }

    private final Logger log = LoggerFactory.getLogger(PaymentCustomerVerificationService.class);

    public PaymentCustomerVerificationService(
            PaymentMutationCoordinator coordinator,
            CustomerVerificationPort customerVerificationPort,
            PaymentCustomerVerificationRequestFactory requestFactory,
            CustomerVerificationEvidenceMapper evidenceMapper,
            CustomerVerificationFailureMapper failureMapper,
            PaymentCustomerVerificationIdGenerator idGenerator,
            ResolvedDebtorAccountReferenceFactory accountReferenceFactory
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
        this.accountReferenceFactory = Objects.requireNonNull(
                accountReferenceFactory,
                "accountReferenceFactory is required"
        );
    }

    public PaymentWorkflowResult verifyCustomer(
            PaymentId paymentId,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return verifyCustomer(paymentId, decisionAt, Instant.MAX, policies);
    }

    public PaymentWorkflowResult verifyCustomer(
            PaymentId paymentId,
            Instant decisionAt,
            Instant deadlineAt,
            PaymentPolicyBundle policies
    ) {
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(decisionAt, "decisionAt is required");
        Objects.requireNonNull(deadlineAt, "deadlineAt is required");
        Objects.requireNonNull(policies, "policies are required");

        UUID verificationId = idGenerator.forPayment(paymentId);
        // This holder is request-local: it contains the exact evidence computed
        // before an optimistic-write conflict, never a second bank invocation.
        BankingVerificationSnapshot[] observed = new BankingVerificationSnapshot[1];
        ReentrantLock gate = VERIFICATION_GATES[Math.floorMod(
                paymentId.value().hashCode(), VERIFICATION_GATES.length)];
        gate.lock();
        try {
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
                                    decisionAt,
                                    deadlineAt
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
                    if (response.outcome()
                            == CustomerVerificationResponse.Outcome.VERIFIED
                            && payment.toState()
                                    .optionalDebtorAccountReference()
                                    .isEmpty()) {
                        payment.resolveDebtorAccount(
                                accountReferenceFactory.from(response),
                                response.completedAt()
                        );
                    }

                    var snapshot = evidenceMapper.toSnapshot(
                            response,
                            payment.toState()
                                    .requestIdentity()
                                    .correlationId(),
                            response.completedAt()
                    );

                    observed[0] = snapshot;

                    var currentAccount = payment.toState()
                            .optionalDebtorAccountReference();

                    boolean bindingMatches = currentAccount
                            .map(account ->
                                    account.bindingFingerprint().equals(
                                            snapshot.accountBindingFingerprint()
                                    ))
                            .orElse(false);
                    log.info(
                            "event=payment_banking_binding_check "
                                    + "paymentId={} "
                                    + "accountPresent={} "
                                    + "bindingMatches={} "
                                    + "outcome={}",
                            paymentId,
                            currentAccount.isPresent(),
                            bindingMatches,
                            response.outcome()
                    );

                    var failure = failureMapper.from(
                            response,
                            response.completedAt()
                    );

                    // Canonical bank-completion time is stable across replay.
                    // It must not precede a debtor-account resolution performed
                    // from the same Customer response.
                    payment.recordBankingVerification(
                            snapshot,
                            failure,
                            response.completedAt(),
                            policies
                    );
                }
            );
        } catch (PaymentConcurrentMutationException conflict) {
            BankingVerificationSnapshot candidate = observed[0];
            if (candidate == null) {
                throw conflict;
            }
            // Re-read after the losing transaction has rolled back. Only
            // acknowledge a *durably recorded and matching* banking result;
            // never replay a banking operation or conceal conflicting evidence.
            return coordinator.mutate(paymentId, current -> {
                BankingVerificationSnapshot durable = current.toState()
                        .bankingVerificationEvidence().orElse(null);
                if (durable == null
                        || !durable.verificationId().equals(candidate.verificationId())
                        || !durable.metadata().evidenceFingerprint().equals(
                                candidate.metadata().evidenceFingerprint())
                        || durable.outcome() != candidate.outcome()
                        || !durable.accountBindingFingerprint().equals(
                                candidate.accountBindingFingerprint())) {
                    throw conflict;
                }
            });
        } finally {
            gate.unlock();
        }
    }
}
