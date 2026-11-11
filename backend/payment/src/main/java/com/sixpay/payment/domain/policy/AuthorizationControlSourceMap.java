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
                        "Partner application surface: ACTIVE status and AuthorizedPerimeter.",
                        AuthorizationControlSource.ImplementationStatus.READY
                )
        );

        result.put(
                AuthorizationControl.SUBSCRIPTION_AUTHORIZED,
                new AuthorizationControlSource(
                        AuthorizationControl.SUBSCRIPTION_AUTHORIZED,
                        AuthorizationSourceKind.REQUIRES_RUNTIME_SOURCE,
                        "customer",
                        "Customer-owned CustomerSubscription eligibility and exact Partner/account binding after OTP.",
                        AuthorizationControlSource.ImplementationStatus.READY
                )
        );

        result.put(
                AuthorizationControl.APPLICATION_AUTHORIZED,
                new AuthorizationControlSource(
                        AuthorizationControl.APPLICATION_AUTHORIZED,
                        AuthorizationSourceKind.REQUIRES_RUNTIME_SOURCE,
                        "security",
                        "Registered machine subject and initiating Payment Partner binding.",
                        AuthorizationControlSource.ImplementationStatus.READY
                )
        );

        result.put(
                AuthorizationControl.CLAIM_TYPE_AUTHORIZED,
                new AuthorizationControlSource(
                        AuthorizationControl.CLAIM_TYPE_AUTHORIZED,
                        AuthorizationSourceKind.PAYMENT_STATE,
                        "partner",
                        "Payment ClaimType must belong to Partner authorized transaction types.",
                        AuthorizationControlSource.ImplementationStatus.READY
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
