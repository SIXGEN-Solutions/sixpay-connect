package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RegionalSecurityContractAlignmentTest {
    @Test
    void outgoingConfirmationContractRequiresMtlsWithoutOauth2() throws Exception {
        Path root = Path.of("../..");
        String physical = Files.readString(root.resolve(
                "documentation/contracts/amplitude/amplitude-payment-confirmation-api-v1.yaml"));
        assertTrue(physical.contains("security:\n  - mutualTLS: []"));
        assertFalse(physical.contains("oauth2ClientCredentials"));
    }

    @Test
    void registryBankingAuthenticationDoesNotRequireOAuth2() throws Exception {
        String yaml = Files.readString(Path.of("../../documentation/contracts/CONTRACT_REGISTRY.yaml"));
        for (String id : new String[] {
                "amplitude-payment-confirmation-api-v1",
                "amplitude-accounting-entries-api-v1",
                "amplitude-payment-posting-api-v1"}) {
            int start = yaml.indexOf("  - id: \"" + id + "\"");
            assertTrue(start >= 0, "missing " + id);
            int next = yaml.indexOf("\n  - id:", start + 5);
            String entry = yaml.substring(start, next < 0 ? yaml.length() : next);
            assertTrue(entry.contains("authentication: [\"MUTUAL_TLS\"]"), id);
            assertFalse(entry.contains("authentication: [\"OAUTH2_CLIENT_CREDENTIALS\""), id);
        }
    }
}
