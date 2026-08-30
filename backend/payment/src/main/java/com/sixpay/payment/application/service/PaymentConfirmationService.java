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
import com.sixpay.payment.application.query.ReadPaymentConfirmationQuery;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Application orchestration for the four public Payment-confirmation
 * operations.
 *
 * <p>This service deliberately does not persist ConfirmationChallenge yet.
 * LOT 1.3 introduces the application and banking boundaries only. Persisting
 * an authoritative challenge snapshot must not bypass the existing Payment
 * atomic mutation/audit invariant and is handled by the later persistence /
 * idempotency lots.</p>
 */
@Service
@ConditionalOnBean(PaymentConfirmationGateway.class)
public class PaymentConfirmationService
        implements CreatePaymentConfirmationUseCase,
        ReadPaymentConfirmationUseCase,
        VerifyPaymentConfirmationUseCase,
        ResendPaymentConfirmationUseCase {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentConfirmationGateway confirmationGateway;

    public PaymentConfirmationService(
            PaymentLookupPort paymentLookupPort,
            PaymentConfirmationGateway confirmationGateway
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
        this.confirmationGateway = Objects.requireNonNull(
                confirmationGateway,
                "Payment confirmation gateway"
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

        PaymentConfirmationBankResult result =
                confirmationGateway.create(
                        new PaymentConfirmationGateway.CreateRequest(
                                payment,
                                context,
                                bankingIdempotencyKey(
                                        command.idempotencyKey().value()
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
            PaymentConfirmationBankResult result =
                    confirmationGateway.verify(
                            new PaymentConfirmationGateway.VerifyRequest(
                                    payment.id(),
                                    payment.publicPaymentReference(),
                                    challenge.challengeReference(),
                                    bankingContext(
                                            payment,
                                            command.correlationId()
                                    ),
                                    bankingIdempotencyKey(
                                            command.idempotencyKey().value()
                                    ),
                                    otp
                            )
                    );

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

        PaymentConfirmationBankResult result =
                confirmationGateway.replace(
                        new PaymentConfirmationGateway.ReplaceRequest(
                                payment.id(),
                                payment.publicPaymentReference(),
                                challenge.challengeReference(),
                                bankingContext(
                                        payment,
                                        command.correlationId()
                                ),
                                bankingIdempotencyKey(
                                        command.idempotencyKey().value()
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
