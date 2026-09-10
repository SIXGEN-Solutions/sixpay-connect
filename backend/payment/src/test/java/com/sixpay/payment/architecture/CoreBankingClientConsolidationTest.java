package com.sixpay.payment.architecture;

import com.sixpay.payment.infrastructure.banking.amplitude.AmplitudeAccountFundsClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.AmplitudePaymentEventClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.AmplitudePaymentEventRecoveryClient;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.AmplitudeReversalClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CoreBankingClientConsolidationTest {

    @Test
    void usesCapabilitySpecificAmplitudeClientsOnly() {
        assertNotNull(AmplitudeAccountFundsClient.class);
        assertNotNull(AmplitudePaymentEventClient.class);
        assertNotNull(AmplitudePaymentEventRecoveryClient.class);
        assertNotNull(AmplitudeReversalClient.class);

        ClassLoader classLoader =
                CoreBankingClientConsolidationTest.class.getClassLoader();

        assertNull(classLoader.getResource(
                "com/sixpay/payment/infrastructure/banking/amplitude/"
                        + "Amplitude" + "BankingClient.class"
        ));
    }
}
