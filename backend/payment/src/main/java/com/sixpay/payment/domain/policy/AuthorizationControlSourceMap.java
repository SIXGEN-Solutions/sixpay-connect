package com.sixpay.payment.domain.policy;

import java.util.EnumMap;
import java.util.Map;

/**
 * Canonical LOT 2.1.1 mapping of the six AUTHORIZATION_CHECKING controls.
 *
 * <p>This class is descriptive only. It cannot approve or reject a Payment.
 * Its purpose is to prevent LOT 2.1.2 from inventing a source of truth.</p>
 */
public final class AuthorizationControlSourceMap {

    private static final Map<
            AuthorizationControl,
            AuthorizationControlSource
    > SOURCES = buildSources();

    private AuthorizationControlSourceMap() {
    }

    public static AuthorizationControlSource sourceFor(
            AuthorizationControl control
    ) {
        AuthorizationControlSource source = SOURCES.get(control);
        if (source == null) {
            throw new IllegalArgumentException(
                    "No source mapping for control " + control
            );
        }
        return source;
    }

    public static Map<
            AuthorizationControl,
            AuthorizationControlSource
    > all() {
        return SOURCES;
    }

    private static Map<
            AuthorizationControl,
            AuthorizationControlSource
    > buildSources() {
        EnumMap<
                AuthorizationControl,
                AuthorizationControlSource
        > result = new EnumMap<>(AuthorizationControl.class);

        result.put(
                AuthorizationControl.PARTNER_AUTHORIZED,
                new AuthorizationControlSource(
                        AuthorizationControl.PARTNER_AUTHORIZED,
                        AuthorizationSourceKind.REQUIRES_RUNTIME_SOURCE,
                        "partner",
                        "Resolved business authority: Partner accepts new "
                                + "transactions only when PartnerStatus.ACTIVE and "
                                + "AuthorizedPerimeter allows the transaction type. "
                                + "PaymentInitiationContext.partnerLoginName is durably "
                                + "retained, but Payment cannot access partner "
                                + "repositories directly and no approved cross-module "
                                + "runtime port keyed by partnerLoginName is demonstrated "
                                + "at this baseline.",
                        AuthorizationControlSource
                                .ImplementationStatus
                                .REQUIRES_RUNTIME_SOURCE
                )
        );

        result.put(
                AuthorizationControl.SUBSCRIPTION_AUTHORIZED,
                new AuthorizationControlSource(
                        AuthorizationControl.SUBSCRIPTION_AUTHORIZED,
                        AuthorizationSourceKind.TRUSTED_INTAKE_ATTESTATION,
                        "TRESOR_PAY",
                        "Resolved source: the short-lived asymmetric TRESOR PAY "
                                + "signed JWT/JWS validated locally at payment intake "
                                + "attests subscription_status=ACTIVE and binds "
                                + "the subscription, client, financial institution, "
                                + "debtor account and payment. "
                                + "CustomerSubscription is not the source; "
                                + "no synchronous TRESOR PAY subscription verification "
                                + "call is allowed in MVP. "
                                + "Implementation remains gated by the "
                                + "tresorpay-payment-request-api-v1 contract approval.",
                        AuthorizationControlSource
                                .ImplementationStatus
                                .REQUIRES_RUNTIME_SOURCE
                )
        );

        result.put(
                AuthorizationControl.APPLICATION_AUTHORIZED,
                new AuthorizationControlSource(
                        AuthorizationControl.APPLICATION_AUTHORIZED,
                        AuthorizationSourceKind.REQUIRES_RUNTIME_SOURCE,
                        "security / TRESOR_PAY intake profile",
                        "Resolved source: PaymentInitiationContext.applicationId "
                                + "comes from X-TresorPay-App-Id and the signed "
                                + "authorization token client_id is required to match "
                                + "that application identity and the Subscription Key "
                                + "owner at intake. Re-evaluation after OTP requires "
                                + "durable canonical acceptance evidence; current "
                                + "PaymentInitiationContext alone proves the identifier "
                                + "but not the prior authorization decision.",
                        AuthorizationControlSource
                                .ImplementationStatus
                                .REQUIRES_RUNTIME_SOURCE
                )
        );

        result.put(
                AuthorizationControl.CLAIM_TYPE_AUTHORIZED,
                new AuthorizationControlSource(
                        AuthorizationControl.CLAIM_TYPE_AUTHORIZED,
                        AuthorizationSourceKind.PAYMENT_STATE,
                        "payment",
                        "Resolved data source: PaymentInitiationContext.claimType "
                                + "durably retains the normalized Payment ClaimType. "
                                + "No active authoritative source at this baseline "
                                + "defines a partner/application-specific entitlement "
                                + "rule for AVI, IM7 or RNF, so the decision rule "
                                + "remains unresolved and must not be invented.",
                        AuthorizationControlSource
                                .ImplementationStatus
                                .REQUIRES_RUNTIME_SOURCE
                )
        );

        result.put(
                AuthorizationControl.EXECUTION_DATE_VALID,
                new AuthorizationControlSource(
                        AuthorizationControl.EXECUTION_DATE_VALID,
                        AuthorizationSourceKind.PAYMENT_STATE,
                        "payment",
                        "Resolved data source: "
                                + "PaymentInitiationContext.requestedExecutionAt is "
                                + "durably retained. No active authoritative source "
                                + "at this baseline defines the allowable execution "
                                + "date/window policy, so the decision rule remains "
                                + "unresolved and must not be invented.",
                        AuthorizationControlSource
                                .ImplementationStatus
                                .REQUIRES_RUNTIME_SOURCE
                )
        );

        result.put(
                AuthorizationControl.REQUEST_DATA_CONSISTENT,
                new AuthorizationControlSource(
                        AuthorizationControl.REQUEST_DATA_CONSISTENT,
                        AuthorizationSourceKind.PAYMENT_STATE,
                        "payment",
                        "PaymentState requestIdentity, requestedAmount, "
                                + "debtorAccountReference, treasuryAllocationIntent, "
                                + "allocationIntentFingerprint, "
                                + "bankingVerificationEvidence and "
                                + "confirmationChallenge bindings.",
                        AuthorizationControlSource
                                .ImplementationStatus
                                .READY
                )
        );

        return Map.copyOf(result);
    }
}
