package com.sixpay.payment.infrastructure.recovery;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.input.RecoverUnknownPaymentT0UseCase;
import com.sixpay.payment.application.port.output.security.PaymentObjectAccessPort;
import com.sixpay.payment.application.security.PaymentAccessDeniedException;
import com.sixpay.payment.application.security.PaymentAccessPolicy;
import com.sixpay.payment.application.security.PaymentAction;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Authorized operator path. Uses R2 claims and the existing lookup-only T0 use case. */
@Service
public final class PaymentT0ManualReconciliationService {
    public record Result(UUID paymentId, String outcome, String paymentStatus) { }

    private final PaymentT0RecoveryClaimStore claims;
    private final PaymentT0RecoveryBatchProperties properties;
    private final RecoverUnknownPaymentT0UseCase recovery;
    private final PaymentObjectAccessPort accessPort;
    private final PaymentAccessPolicy accessPolicy;
    private final CurrentUserProvider users;
    private final PaymentT0ManualAuditStore audit;
    private final TimeProvider time;

    public PaymentT0ManualReconciliationService(PaymentT0RecoveryClaimStore claims,
            PaymentT0RecoveryBatchProperties properties, RecoverUnknownPaymentT0UseCase recovery,
            PaymentObjectAccessPort accessPort, PaymentAccessPolicy accessPolicy,
            CurrentUserProvider users, PaymentT0ManualAuditStore audit, TimeProvider time) {
        this.claims = Objects.requireNonNull(claims);
        this.properties = Objects.requireNonNull(properties);
        this.recovery = Objects.requireNonNull(recovery);
        this.accessPort = Objects.requireNonNull(accessPort);
        this.accessPolicy = Objects.requireNonNull(accessPolicy);
        this.users = Objects.requireNonNull(users);
        this.audit = Objects.requireNonNull(audit);
        this.time = Objects.requireNonNull(time);
    }

    public List<Result> reconcile(List<UUID> ids, UUID correlationId) {
        Objects.requireNonNull(ids, "paymentIds");
        Objects.requireNonNull(correlationId, "correlationId");
        if (ids.isEmpty() || ids.size() > 50 || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("paymentIds must contain between 1 and 50 non-null IDs");
        }
        Set<UUID> distinct = new HashSet<>(ids);
        if (distinct.size() != ids.size()) {
            throw new IllegalArgumentException("paymentIds must be distinct");
        }
        properties.validate();
        String actor = users.requireCurrentUser().subject();
        // Authorize the entire selection before any banking lookup, avoiding partial
        // processing when a later selected object belongs outside the actor scope.
        for (UUID id : ids) {
            PaymentId paymentId = new PaymentId(id);
            try {
                var descriptor = accessPort.findAccessDescriptor(paymentId)
                        .orElseThrow(() -> new PaymentAccessDeniedException("Payment unavailable"));
                accessPolicy.requireObjectAccess(PaymentAction.RECONCILE, descriptor);
            } catch (PaymentAccessDeniedException denied) {
                audit.record(id, actor, correlationId, "DENIED", time.now());
                throw denied;
            }
        }
        List<Result> outcomes = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            PaymentId paymentId = new PaymentId(id);
            var claim = claims.claimSelected(id, time.now(), properties, "manual:" + UUID.randomUUID());
            if (claim.isEmpty()) {
                outcomes.add(new Result(id, "NOT_ELIGIBLE_OR_BUSY", null));
                audit.record(id, actor, correlationId, "NOT_ELIGIBLE_OR_BUSY", time.now());
                continue;
            }
            var selected = claim.orElseThrow();
            String result;
            String paymentStatus;
            try {
                var outcome = recovery.recoverByPaymentId(paymentId,
                        CorrelationId.of(correlationId.toString()));
                boolean inconclusive = outcome.status() == PaymentStatus.POSTING_PENDING
                        || outcome.status() == PaymentStatus.POSTING_OUTCOME_UNKNOWN;
                boolean owned = inconclusive
                        ? claims.retry(selected, time.now(), PaymentT0RecoveryWorker.delay(selected.attempt()))
                        : claims.completed(selected, time.now());
                result = !owned ? "STALE_CLAIM" : inconclusive ? "RETRY_SCHEDULED" : "RESOLVED";
                paymentStatus = outcome.status().name();
            } catch (Exception exception) {
                boolean owned = claims.retry(selected, time.now(),
                        PaymentT0RecoveryWorker.delay(selected.attempt()));
                result = owned ? "LOOKUP_FAILED_RETRY_SCHEDULED" : "STALE_CLAIM";
                paymentStatus = null;
            }
            // Audit failure must not be reinterpreted as a banking lookup failure.
            audit.record(id, actor, correlationId, result, time.now());
            outcomes.add(new Result(id, result, paymentStatus));
        }
        return List.copyOf(outcomes);
    }
}
