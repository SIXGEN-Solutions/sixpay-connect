package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaim;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntry;
import com.sixpay.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentOutboxMessageSourceTest {
    @Test
    void relaysClaimedPaymentEventWithStableIdentity() {
        var claims = mock(PaymentOutboxClaimService.class);
        var completion = mock(PaymentOutboxCompletionService.class);
        var audit = mock(PaymentAuditAdapter.class);
        var source = new PaymentOutboxMessageSource(claims, completion, audit);
        var now = Instant.parse("2026-09-20T12:00:00Z");
        var eventId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var correlationId = UUID.randomUUID().toString();
        var claim = new PaymentOutboxClaim(eventId, paymentId, "PAYMENT",
                PaymentDomainEventMapper.PAYMENT_DOMAIN_EVENT_TYPE, 1, correlationId,
                "{\"ok\":true}", now, 1, now, "worker");
        when(audit.findByPaymentId(paymentId)).thenReturn(List.of(
                new PaymentAuditEntry(eventId, paymentId, "PAY-TEST",
                        "PaymentFundsControlRequested", PaymentStatus.FUNDS_CONTROL_PENDING,
                        1, 1, correlationId, null, now)));
        when(claims.claimAvailable(eq(now), eq(now.minusSeconds(30)), eq(10), anyString()))
                .thenReturn(List.of(claim));
        List<OutboxMessage> messages = source.claimPending(10, now, Duration.ofSeconds(30));
        assertEquals("payment", source.sourceName());
        assertEquals(1, messages.size());
        assertEquals(eventId, messages.getFirst().event().eventId());
        assertEquals(paymentId, messages.getFirst().event().aggregateId());
        assertEquals("PaymentFundsControlRequested", messages.getFirst().event().eventType());
        assertEquals(correlationId, messages.getFirst().event().correlationId());
        verify(audit).findByPaymentId(paymentId);
        assertEquals(1, messages.getFirst().attemptCount());
        source.markPublished(eventId, now);
        verify(completion).markPublished(eq(eventId), anyString(), eq(now));
        source.markFailed(eventId, "retry", now, now.plusSeconds(5));
        verify(completion).markRetryableFailure(eq(eventId), anyString(), eq("retry"),
                eq(now), eq(now.plusSeconds(5)));
        source.markDead(eventId, "dead", now);
        verify(completion).markDead(eq(eventId), anyString(), eq("dead"), eq(now));
    }

    @Test
    void rejectsMissingAuditInsteadOfSilentlyPublishingWrongType() {
        var claims = mock(PaymentOutboxClaimService.class);
        var completion = mock(PaymentOutboxCompletionService.class);
        var audit = mock(PaymentAuditAdapter.class);
        var source = new PaymentOutboxMessageSource(claims, completion, audit);
        var now = Instant.parse("2026-09-20T12:00:00Z");
        var eventId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        when(claims.claimAvailable(eq(now), eq(now.minusSeconds(30)), eq(10), anyString()))
                .thenReturn(List.of(new PaymentOutboxClaim(eventId, paymentId, "PAYMENT",
                        PaymentDomainEventMapper.PAYMENT_DOMAIN_EVENT_TYPE, 1,
                        UUID.randomUUID().toString(), "{\"ok\":true}",
                        now, 1, now, "worker")));
        when(audit.findByPaymentId(paymentId)).thenReturn(List.of());
        assertThrows(IllegalStateException.class,
                () -> source.claimPending(10, now, Duration.ofSeconds(30)));
        verifyNoInteractions(completion);
    }
}
