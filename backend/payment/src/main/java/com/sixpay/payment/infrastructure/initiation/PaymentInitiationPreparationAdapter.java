package com.sixpay.payment.infrastructure.initiation;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.output.initiation.PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.output.initiation.PreparedPaymentInitiation;
import com.sixpay.payment.application.port.output.customer.PaymentCustomerSubscriptionPort;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.domain.model.CallbackEndpoint;
import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.ExternalSubscriptionReference;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentRequestIdentity;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.RequestFingerprint;
import com.sixpay.payment.domain.model.TreasuryAllocationIntent;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds the domain-safe representation of a new partner debit request.
 *
 * <p>Initiation creates a Payment without any beneficiary allocation. Treasury
 * account resolution happens later in the Payment workflow. This adapter
 * prepares state only; aggregate transitions and persistence remain the
 * responsibility of the application workflow.</p>
 */
@Component
public final class PaymentInitiationPreparationAdapter
        implements PaymentInitiationPreparationPort {

    private final IdentifierGenerator<UUID> identifierGenerator;
    private final PaymentCustomerSubscriptionPort subscriptions;
    private final PartnerIdentityResolutionPort partners;

    public PaymentInitiationPreparationAdapter(
            IdentifierGenerator<UUID> identifierGenerator,
            PaymentCustomerSubscriptionPort subscriptions,
            PartnerIdentityResolutionPort partners
    ) {
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator);
        this.subscriptions = Objects.requireNonNull(subscriptions);
        this.partners = Objects.requireNonNull(partners);
    }

    /**
     * Generates Payment identities and translates the command into a
     * {@link NewPaymentIntent}. The request hash is retained independently from
     * the canonical empty-allocation fingerprint. Actual Treasury configuration
     * must never be supplied or fabricated by the initiating partner.
     */
    @Override
    public PreparedPaymentInitiation prepare(
            InitiateDebitCommand command,
            String requestHash,
            Instant receivedAt
    ) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(receivedAt);
        requireSha256(requestHash);

        UUID generatedId = identifierGenerator.generate();
        PaymentId paymentId = new PaymentId(generatedId);

        FinancialInstitutionCode institution = null;

        Money total = Money.of(
                command.totalAmount(),
                command.currency()
        );

        TreasuryAllocationIntent allocations =
                new TreasuryAllocationIntent(List.of(), total);

        // Stable sentinel fingerprint of the unallocated initiation state.
        // It is NOT evidence of an approved Treasury beneficiary configuration.
        String allocationHash = sha256("");

        NewPaymentIntent intent = new NewPaymentIntent(
                PaymentSource.TRESOR_PAY,
                ExternalPaymentReference.of(command.endToEndId()),
                ExternalSubscriptionReference.of(
                        subscriptionReference(command)
                ),
                new PaymentRequestIdentity(
                        IdempotencyKey.of(command.idempotencyKey()),
                        RequestFingerprint.of(requestHash),
                        command.correlationId()
                ),
                institution,
                null,
                total,
                allocations,
                EvidenceFingerprint.of(
                        "v1:sha256:" + allocationHash
                ),
                new PaymentInitiationContext(
                        // Persist the authenticated OAuth2 client identity, never LoginName.
                        // The legacy field name is retained for state compatibility.
                        command.authenticatedPartnerLoginName(),
                        command.resolvedPartnerIdentifier(),
                        command.debtorName(),
                        command.claimType(),
                        command.taxpayerIdentifier(),
                        command.requestedExecutionAt(),
                        CallbackEndpoint.of(command.callbackUrl())
                )
        );

        return new PreparedPaymentInitiation(
                paymentId,
                publicReference(generatedId),
                intent,
                receivedAt
        );
    }

    /** Resolve the public banking reference before persisting the internal UUID.
     * All downstream Payment, banking-verification and OTP logic stays unchanged.
     */
    private String subscriptionReference(InitiateDebitCommand command) {
        var partner = partners.findByPartnerIdentifier(command.resolvedPartnerIdentifier())
                .orElseThrow(() -> new IllegalStateException("Payment Partner is unavailable"));
        if (!partner.acceptsNewTransactions()) {
            throw new IllegalStateException("Payment Partner is not authorized");
        }
        return subscriptions.resolveByBankingCustomerReference(
                command.clientReference(), partner.partnerId())
                .subscriptionId().toString();
    }

    private static PublicPaymentReference publicReference(
            UUID identifier
    ) {
        String encoded = identifier.toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase(Locale.ROOT);

        return PublicPaymentReference.of("PAY-" + encoded);
    }

    private static void requireSha256(String value) {
        if (value == null
                || !value.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Request hash must contain 64 lowercase hexadecimal characters"
            );
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}
