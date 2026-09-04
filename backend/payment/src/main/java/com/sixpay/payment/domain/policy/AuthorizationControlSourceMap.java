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
                        "PaymentInitiationContext.partnerLoginName is persisted; "
                                + "PartnerStatus.ACTIVE is owned by partner, but no "
                                + "approved Payment runtime lookup by partnerLoginName "
                                + "is demonstrated.",
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
                        "Short-lived signed JWT validated locally at payment intake "
                                + "attests the external subscription; "
                                + "CustomerSubscription is not the source and no "
                                + "synchronous TRESOR PAY subscription verification "
                                + "is allowed in MVP.",
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
                        "PaymentInitiationContext.applicationId is persisted and "
                                + "originates from the authenticated TRESOR PAY request; "
                                + "no approved post-OTP runtime "
                                + "application-authorization source is demonstrated.",
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
                        "PaymentInitiationContext.claimType retains normalized "
                                + "Payment ClaimType (AVI, IM7, RNF); any "
                                + "partner/application-specific entitlement rule "
                                + "remains unresolved.",
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
                        "PaymentInitiationContext.requestedExecutionAt is durably "
                                + "retained; the concrete execution-date policy is "
                                + "not yet defined by active sources.",
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
