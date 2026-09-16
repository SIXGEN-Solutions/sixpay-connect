package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.*;
import com.sixpay.payment.application.port.output.idempotency.PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.output.initiation.*;
import com.sixpay.payment.application.view.*;
import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentInitiationOrchestrationServiceTest {
    private static final Instant NOW=Instant.parse("2026-08-03T10:30:00Z");
    @Test void continuesSynchronouslyUntilActiveOtpChallenge() {
        PaymentInitiationIdempotencyPort idem=Mockito.mock(PaymentInitiationIdempotencyPort.class); PaymentInitiationPreparationPort prep=Mockito.mock(PaymentInitiationPreparationPort.class); PaymentReceptionService reception=Mockito.mock(PaymentReceptionService.class); PaymentMutationCoordinator coordinator=Mockito.mock(PaymentMutationCoordinator.class); PaymentCustomerVerificationService customer=Mockito.mock(PaymentCustomerVerificationService.class); PaymentConfirmationService confirmation=Mockito.mock(PaymentConfirmationService.class); PaymentPolicyBundle policies=Mockito.mock(PaymentPolicyBundle.class);
        @SuppressWarnings("unchecked") ObjectProvider<PaymentCustomerVerificationService> cp=Mockito.mock(ObjectProvider.class); @SuppressWarnings("unchecked") ObjectProvider<PaymentConfirmationService> fp=Mockito.mock(ObjectProvider.class);
        InitiateDebitCommand command=command(); PaymentId id=new PaymentId(UUID.randomUUID()); PublicPaymentReference ref=PublicPaymentReference.of("PAY-1234567890ABCDEFGHJKMNPQRS"); NewPaymentIntent intent=Mockito.mock(NewPaymentIntent.class);
        when(idem.execute(any(),any())).thenAnswer(i->{ @SuppressWarnings("unchecked") Function<String,InitiateDebitResult> f=i.getArgument(1); return f.apply("a".repeat(64)); });
        when(prep.prepare(command,"a".repeat(64),NOW)).thenReturn(new PreparedPaymentInitiation(id,ref,intent,NOW));
        when(reception.receive(id,ref,intent,NOW)).thenReturn(flow(id,ref,PaymentStatus.RECEIVED,1));
        when(coordinator.mutate(any(),any())).thenReturn(flow(id,ref,PaymentStatus.BANKING_VERIFICATION_PENDING,2));
        when(cp.getIfAvailable()).thenReturn(customer); when(customer.verifyCustomer(any(),any(),any(),any())).thenReturn(flow(id,ref,PaymentStatus.PENDING_CONFIRMATION,3));
        PaymentConfirmationView challenge=new PaymentConfirmationView(ref,ConfirmationChallengeStatus.ACTIVE,ConfirmationBusinessCode.CHALLENGE_ACTIVE,null,NOW,NOW.plusSeconds(300),null);
        when(fp.getIfAvailable()).thenReturn(confirmation); when(confirmation.create(any())).thenReturn(challenge);
        InitiateDebitResult result=new PaymentInitiationOrchestrationService(idem,prep,reception,coordinator,cp,fp,policies,()->NOW,new PaymentInitiationDeadline(java.time.Duration.ofSeconds(30))).initiateDebit(command);
        assertThat(result.status()).isEqualTo(InitiateDebitStatus.AWAITING_OTP); assertThat(result.confirmationChallenge().status()).isEqualTo(ConfirmationChallengeStatus.ACTIVE);
        Mockito.verify(confirmation, Mockito.times(1)).create(any());
    }
    private static PaymentWorkflowResult flow(PaymentId id,PublicPaymentReference ref,PaymentStatus s,long v){ return new PaymentWorkflowResult(id,ref,s,v,true); }
    private static InitiateDebitCommand command(){ return new InitiateDebitCommand("TRESOR_PAY","TRESOR_PAY","TP_APP_001","AVI-2025-00045678",new BigDecimal("600000"),"XAF","10005-00001-12345678901-12","Société ABC SARL",ClaimType.AVI,"100200300",NOW,List.of(new InitiateDebitBeneficiaryCommand("10005-00001-TRESDGI-97",new BigDecimal("600000"))),"https://tresorpay.cm/callback","IDEMPOTENCY-00000001",CorrelationId.of("11111111-1111-1111-1111-111111111111")); }
}
