package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.partner.AuthenticatedPartnerCallerPort;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Payment-owned object-level authorization for the three public confirmation
 * operations. Called at the HTTP boundary and at the application
 * use-case boundary, before idempotency or bank I/O.
 * Neither an HTTP partner header nor an untrusted public reference authorizes
 * access by itself. The original Payment partner binding is authoritative.
 */
@Component("paymentConfirmationPartnerAccessGuard")
public final class PaymentConfirmationPartnerAccessGuard {
    private final AuthenticatedPartnerCallerPort caller;
    private final PartnerIdentityAlignmentService alignment;
    private final PaymentLookupPort payments;

    public PaymentConfirmationPartnerAccessGuard(
            AuthenticatedPartnerCallerPort caller,
            PartnerIdentityAlignmentService alignment,
            PaymentLookupPort payments) {
        this.caller = Objects.requireNonNull(caller);
        this.alignment = Objects.requireNonNull(alignment);
        this.payments = Objects.requireNonNull(payments);
    }

    public boolean isAllowed(String publicPaymentReference) {
        // Always deny when the transport has not established a machine identity.
        String subject = caller.currentAuthenticatedSubject()
                .filter(value -> !value.isBlank()).orElse(null);
        if (subject == null || publicPaymentReference == null) return false;
        try {
            var payment = payments.findByPublicPaymentReference(
                    PublicPaymentReference.of(publicPaymentReference));
            if (payment.isEmpty()) return false;
            var context = payment.orElseThrow().toState().initiationContext()
                    .orElse(null);
            if (context == null) return false;
            String owner = context.applicationId();
            String initiatingMachine = context.partnerLoginName();
            if (owner == null || owner.isBlank()
                    || initiatingMachine == null || initiatingMachine.isBlank()) return false;
            // Strict initiating OAuth2 client isolation. The stored value
            // comes from authenticated Security context at initiation.
            // Older records without that trusted binding are denied.
            if (!subject.equals(initiatingMachine)) return false;
            // Reuses the same registered machine -> Partner ID resolution as initiateDebit.
            // The stored owner is checked against the authenticated Partner ID.
            alignment.requireConsistentPartner(subject, owner);
            return true;
        } catch (PartnerIdentityResolutionException | IllegalArgumentException exception) {
            return false;
        }
    }
}
