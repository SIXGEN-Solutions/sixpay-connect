package com.sixpay.payment.architecture;

import com.sixpay.payment.infrastructure.banking.amplitude.AmplitudeAccountFundsClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePostingClient;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.AmplitudeReversalClient;
import com.sixpay.payment.infrastructure.banking.amplitude.status.AmplitudePostingStatusClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CoreBankingClientConsolidationTest {

    @Test
    void usesCapabilitySpecificAmplitudeClientsOnly() {
        assertNotNull(AmplitudeAccountFundsClient.class);
        assertNotNull(AmplitudePostingClient.class);
        assertNotNull(AmplitudePostingStatusClient.class);
        assertNotNull(AmplitudeReversalClient.class);

        ClassLoader classLoader =
                CoreBankingClientConsolidationTest.class.getClassLoader();

        assertNull(classLoader.getResource(
                "com/sixpay/payment/infrastructure/banking/amplitude/"
                        + "Amplitude" + "BankingClient.class"
        ));
    }
}
