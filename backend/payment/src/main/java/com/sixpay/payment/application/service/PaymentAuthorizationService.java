package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.AuthorizationEvidenceSnapshot;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.SixpayAuthorizationGate;
import com.sixpay.payment.domain.policy.SixpayAuthorizationGateResult;
import com.sixpay.payment.application.port.PartnerAuthorizationPort;
import com.sixpay.payment.domain.policy.PartnerAuthorizationView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates authorization and banking-verification evidence.
 */
@Service
public class PaymentAuthorizationService {

    private final PaymentMutationCoordinator coordinator;
    private final SixpayAuthorizationGate authorizationGate;
    private final Optional<PartnerAuthorizationPort> partnerAuthorizationPort;

    @Autowired
    public PaymentAuthorizationService(
            PaymentMutationCoordinator coordinator,
            Optional<PartnerAuthorizationPort> partnerAuthorizationPort
    ) {
        this(
                coordinator,
                new SixpayAuthorizationGate(),
                partnerAuthorizationPort
        );
    }

    PaymentAuthorizationService(
            PaymentMutationCoordinator coordinator,
            SixpayAuthorizationGate authorizationGate,
            PartnerAuthorizationPort partnerAuthorizationPort
    ) {
        this(
                coordinator,
                authorizationGate,
                Optional.of(
                        Objects.requireNonNull(
                                partnerAuthorizationPort,
                                "Partner authorization port"
                        )
                )
        );
    }

    PaymentAuthorizationService(
            PaymentMutationCoordinator coordinator,
            SixpayAuthorizationGate authorizationGate,
            Optional<PartnerAuthorizationPort> partnerAuthorizationPort
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
        this.authorizationGate = Objects.requireNonNull(
                authorizationGate,
                "SIXPAY authorization gate"
        );
        this.partnerAuthorizationPort = Objects.requireNonNull(
                partnerAuthorizationPort,
                "Partner authorization port"
        );
    }

    public PaymentWorkflowResult attachConfirmationChallenge(
            PaymentId paymentId,
            ConfirmationChallenge challenge,
            Instant observedAt
    ) {
        Objects.requireNonNull(
                challenge,
                "Confirmation challenge"
        );
        Objects.requireNonNull(
                observedAt,
                "Confirmation challenge observation instant"
        );
        return coordinator.mutate(
                paymentId,
                payment -> payment.recordConfirmationChallenge(
                        challenge,
                        observedAt
                )
        );
    }

    public PaymentWorkflowResult startAuthorization(
            PaymentId paymentId,
            ConfirmationChallenge verifiedChallenge
    ) {
        Objects.requireNonNull(
                verifiedChallenge,
                "Verified confirmation challenge"
        );

        return coordinator.mutate(
                paymentId,
                payment -> {
                    payment.recordCustomerConfirmation(
                            verifiedChallenge
                    );

                    var state = payment.toState();
                    PartnerAuthorizationView partnerAuthorization =
                            partnerAuthorizationPort
                                    .map(port -> port.resolve(
                                            state.initiationContext()
                                                    .orElseThrow()
                                    ))
                                    .orElse(null);

                    SixpayAuthorizationGateResult result =
                            authorizationGate.evaluate(
                                    state,
                                    partnerAuthorization
                            );

                    if (result.rejected()) {
                        throw new IllegalStateException(
                                "SIXPAY authorization gate rejected "
                                        + "the Payment after OTP verification"
                        );
                    }

                    if (result.incomplete()) {
                        return;
                    }

                    payment.approveSixpayAuthorization(
                            verifiedChallenge
                                    .optionalVerifiedAt()
                                    .orElseThrow()
                    );
                }
        );
    }

    public PaymentWorkflowResult startAuthorization(
            PaymentId paymentId,
            Instant startedAt
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.startAuthorizationChecking(
                                startedAt
                        )
        );
    }

    public PaymentWorkflowResult recordAuthorizationDecision(
            PaymentId paymentId,
            AuthorizationEvidenceSnapshot evidence,
            PaymentFailure rejectionFailure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordAuthorizationDecision(
                                evidence,
                                rejectionFailure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult recordBankingVerification(
            PaymentId paymentId,
            BankingVerificationSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordBankingVerification(
                                evidence,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }
}
