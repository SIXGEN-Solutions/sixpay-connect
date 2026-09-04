package com.sixpay.payment.infrastructure.banking.amplitude.confirmation;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DedicatedAmplitudePaymentConfirmationAdapterTest {

    private final AmplitudePaymentConfirmationClient client =
            mock(AmplitudePaymentConfirmationClient.class);
    private final DedicatedAmplitudePaymentConfirmationAdapter adapter =
            new DedicatedAmplitudePaymentConfirmationAdapter(client);

    @Test
    void delegatesAllApprovedGatewayOperations() {
        PaymentConfirmationGateway.CreateRequest create =
                mock(PaymentConfirmationGateway.CreateRequest.class);
        PaymentConfirmationGateway.VerifyRequest verify =
                mock(PaymentConfirmationGateway.VerifyRequest.class);
        PaymentConfirmationGateway.ReplaceRequest replace =
                mock(PaymentConfirmationGateway.ReplaceRequest.class);
        PaymentConfirmationGateway.LookupRequest lookup =
                mock(PaymentConfirmationGateway.LookupRequest.class);
        PaymentConfirmationGateway.RecoveryRequest recover =
                mock(PaymentConfirmationGateway.RecoveryRequest.class);
        PaymentConfirmationGateway.RevokeRequest revoke =
                mock(PaymentConfirmationGateway.RevokeRequest.class);
        PaymentConfirmationBankResult result =
                mock(PaymentConfirmationBankResult.class);

        when(client.create(create)).thenReturn(result);
        when(client.verify(verify)).thenReturn(result);
        when(client.replace(replace)).thenReturn(result);
        when(client.lookup(lookup)).thenReturn(result);
        when(client.recover(recover)).thenReturn(result);
        when(client.revoke(revoke)).thenReturn(result);

        assertSame(result, adapter.create(create));
        assertSame(result, adapter.verify(verify));
        assertSame(result, adapter.replace(replace));
        assertSame(result, adapter.lookup(lookup));
        assertSame(result, adapter.recover(recover));
        assertSame(result, adapter.revoke(revoke));
    }
}
