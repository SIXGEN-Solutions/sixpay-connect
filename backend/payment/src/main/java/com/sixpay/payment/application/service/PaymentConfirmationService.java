package com.sixpay.payment.application.service;
import com.sixpay.payment.application.command.CreatePaymentConfirmationCommand;
import com.sixpay.payment.application.command.ResendPaymentConfirmationCommand;
import com.sixpay.payment.application.command.VerifyPaymentConfirmationCommand;
import com.sixpay.payment.application.port.input.CreatePaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ReadPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ResendPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.VerifyPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.query.ReadPaymentConfirmationQuery;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import java.util.Objects;
/** Application orchestration for Payment-confirmation operations. */
@Service
@ConditionalOnBean({
        PaymentConfirmationGateway.class,
        PaymentConfirmationIdempotencyPort.class
})
public class PaymentConfirmationService
        implements CreatePaymentConfirmationUseCase,
        ReadPaymentConfirmationUseCase,
        VerifyPaymentConfirmationUseCase,
        ResendPaymentConfirmationUseCase {
    private final PaymentLookupPort paymentLookupPort;
    private final PaymentAuthorizationService authorizationService;
    private final PaymentConfirmationGateway confirmationGateway;
    private final PaymentConfirmationIdempotencyPort idempotencyPort;
    public PaymentConfirmationService(
            PaymentLookupPort paymentLookupPort,
            PaymentAuthorizationService authorizationService,
            PaymentConfirmationGateway confirmationGateway,
            PaymentConfirmationIdempotencyPort idempotencyPort
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
        this.authorizationService = Objects.requireNonNull(
                authorizationService,
                "Payment authorization service"
        );
        this.confirmationGateway = Objects.requireNonNull(
                confirmationGateway,
                "Payment confirmation gateway"
        );
        this.idempotencyPort = Objects.requireNonNull(
                idempotencyPort,
                "Payment confirmation idempotency port"
        );
    }
    @Override
    public PaymentConfirmationView create(
            CreatePaymentConfirmationCommand command
    ) {
        Objects.requireNonNull(command, "Create confirmation command");
        Payment payment = requirePayment(command.paymentReference());
        requirePendingConfirmation(payment);
        payment.toState()
                .confirmationChallenge()
                .filter(ConfirmationChallenge::active)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Payment already has an active confirmation challenge"
                    );
                });
        BankingRequestContext context = bankingContext(
                payment,
                command.correlationId()
        );
        BankingIdempotencyKey bankKey =
                bankingIdempotencyKey(
                        command.idempotencyKey().value()
                );
        PaymentConfirmationBankResult result =
                idempotencyPort.executeCreate(
                        payment.id(),
                        payment.publicPaymentReference(),
                        command.idempotencyKey(),
                        () -> confirmationGateway.create(
                                new PaymentConfirmationGateway.CreateRequest(
                                        payment,
                                        context,
                                        bankKey
                                )
                        ),
                        () -> confirmationGateway.recover(
                                new PaymentConfirmationGateway.RecoveryRequest(
                                        context,
                                        bankKey
                                )
                        )
                );
        return PaymentConfirmationView.from(
                payment.publicPaymentReference(),
                result
        );
    }
    @Override
    public PaymentConfirmationView read(
            ReadPaymentConfirmationQuery query
    ) {
        Objects.requireNonNull(query, "Read confirmation query");
        Payment payment = requirePayment(query.paymentReference());
        ConfirmationChallenge challenge =
                requireCurrentChallenge(payment);
        PaymentConfirmationBankResult result =
                confirmationGateway.lookup(
                        new PaymentConfirmationGateway.LookupRequest(
                                payment.id(),
                                payment.publicPaymentReference(),
                                challenge.challengeReference(),
                                bankingContext(
                                        payment,
                                        query.correlationId()
                                )
                        )
                );
        return PaymentConfirmationView.from(
                payment.publicPaymentReference(),
                result
        );
    }
    @Override
    public PaymentConfirmationView verify(
            VerifyPaymentConfirmationCommand command
    ) {
        Objects.requireNonNull(command, "Verify confirmation command");
        Payment payment = requirePayment(command.paymentReference());
        requirePendingConfirmation(payment);
        ConfirmationChallenge challenge =
                requireCurrentChallenge(payment);
        char[] otp = command.otp();
        try {
            BankingRequestContext context =
                    bankingContext(
                            payment,
                            command.correlationId()
                    );
            BankingIdempotencyKey bankKey =
                    bankingIdempotencyKey(
                            command.idempotencyKey().value()
                    );
            PaymentConfirmationBankResult result =
                    idempotencyPort.executeVerify(
                            payment.id(),
                            payment.publicPaymentReference(),
                            command.idempotencyKey(),
                            otp,
                            () -> confirmationGateway.verify(
                                    new PaymentConfirmationGateway.VerifyRequest(
                                            payment.id(),
                                            payment.publicPaymentReference(),
                                            challenge.challengeReference(),
                                            context,
                                            bankKey,
                                            otp
                                    )
                            )
                    );
            if (result.status()
                    == com.sixpay.payment.domain.model
                            .ConfirmationChallengeStatus.VERIFIED) {
                java.time.Instant verifiedAt =
                    result.optionalVerifiedAt().orElseThrow(
                            () -> new IllegalStateException(
                                    "VERIFIED confirmation requires verifiedAt"
                            )
                    );

            ConfirmationChallenge verifiedChallenge =
                    new ConfirmationChallenge(
                            result.challengeReference(),
                            challenge.binding(),
                            result.status(),
                            result.businessCode(),
                            result.deliveryChannel(),
                            result.sentAt(),
                            result.expiresAt(),
                            verifiedAt
                    );

            authorizationService.startAuthorization(
                    payment.id(),
                    verifiedChallenge
            );
            }
            return PaymentConfirmationView.from(
                    payment.publicPaymentReference(),
                    result
            );
        } finally {
            java.util.Arrays.fill(otp, '\0');
        }
    }
    @Override
    public PaymentConfirmationView resend(
            ResendPaymentConfirmationCommand command
    ) {
        Objects.requireNonNull(command, "Resend confirmation command");
        Payment payment = requirePayment(command.paymentReference());
        requirePendingConfirmation(payment);
        ConfirmationChallenge challenge =
                requireCurrentChallenge(payment);
        BankingRequestContext context =
                bankingContext(
                        payment,
                        command.correlationId()
                );
        BankingIdempotencyKey bankKey =
                bankingIdempotencyKey(
                        command.idempotencyKey().value()
                );
        PaymentConfirmationBankResult result =
                idempotencyPort.executeReplace(
                        payment.id(),
                        payment.publicPaymentReference(),
                        challenge.challengeReference(),
                        command.idempotencyKey(),
                        () -> confirmationGateway.replace(
                                new PaymentConfirmationGateway.ReplaceRequest(
                                        payment.id(),
                                        payment.publicPaymentReference(),
                                        challenge.challengeReference(),
                                        context,
                                        bankKey
                                )
                        ),
                        () -> confirmationGateway.recover(
                                new PaymentConfirmationGateway.RecoveryRequest(
                                        context,
                                        bankKey
                                )
                        )
                );
        return PaymentConfirmationView.from(
                payment.publicPaymentReference(),
                result
        );
    }
    private Payment requirePayment(
            PublicPaymentReference paymentReference
    ) {
        return paymentLookupPort
                .findByPublicPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found: " + paymentReference
                ));
    }
    private static void requirePendingConfirmation(Payment payment) {
        if (payment.status() != PaymentStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException(
                    "Payment confirmation operation requires "
                            + "PENDING_CONFIRMATION"
            );
        }
    }
    private static ConfirmationChallenge requireCurrentChallenge(
            Payment payment
    ) {
        return payment.toState()
                .confirmationChallenge()
                .orElseThrow(() -> new IllegalStateException(
                        "Payment has no current confirmation challenge"
                ));
    }
    private static BankingRequestContext bankingContext(
            Payment payment,
            com.sixpay.common.context.CorrelationId correlationId
    ) {
        return new BankingRequestContext(
                correlationId,
                payment.toState().financialInstitutionCode()
        );
    }
    private static BankingIdempotencyKey bankingIdempotencyKey(
            String value
    ) {
        return new BankingIdempotencyKey(value);
    }
}
