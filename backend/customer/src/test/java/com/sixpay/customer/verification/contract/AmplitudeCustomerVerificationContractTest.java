package com.sixpay.customer.verification.contract;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmplitudeCustomerVerificationContractTest {
    private static final Path CONTRACT = Path.of(
            "../../documentation/contracts/integration/amplitude-customer-verification-api-v1.yaml"
    );

    @Test
    void contractIsVersionedProvisionalAndSecurityHardened() throws Exception {
        String source = Files.readString(CONTRACT);
        for (String required : List.of(
                "openapi: 3.1.0", "version: 1.0.0-provisional",
                "x-sixpay-contract-status: provisional", "/v1/accounts/verify:",
                "mutualTls:", "oauth2ClientCredentials:", "X-Correlation-ID",
                "accountReference", "CUSTOMER_EXISTS", "REQUIRED_KYC_VERIFIED",
                "'502':", "'503':", "'504':"
        )) assertTrue(source.contains(required), () -> "Missing: " + required);
    }

    @Test
    void contractContainsNoCredentialOrLiveEndpoint() throws Exception {
        String source = Files.readString(CONTRACT);
        for (String forbidden : List.of(
                "sk_live_", "LoginName:", "APIKey:", "api.banque.com",
                "clientSecret:", "privateKey:", "password:"
        )) assertFalse(source.contains(forbidden), () -> "Forbidden: " + forbidden);
    }
}
