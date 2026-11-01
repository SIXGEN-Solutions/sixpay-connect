package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CB-4: guard existing separation without changing authentication semantics. */
class CustomerBankingProfileSeparationTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void bankingSandboxImportsOnlyBankingCapability() throws Exception {
        String profile = read("application-amplitude-sandbox.yml");
        assertTrue(profile.contains("on-profile: amplitude-sandbox"));
        assertTrue(profile.contains("classpath:application-customer-banking.yml"));
        assertFalse(profile.contains("classpath:config/security/"));
        assertFalse(profile.contains("authentication:"));
    }

    @Test
    void customerBankingSettingsDoNotEnableEndUserAuthentication() throws Exception {
        String banking = read("application-customer-banking.yml");

        // La capacité bancaire Customer reste configurable.
        assertTrue(banking.contains("verification:"));
        assertTrue(banking.contains("banking:"));
        assertTrue(banking.contains("enabled:"));

        // Aucun mécanisme OAuth2 ou mTLS technique ne doit être réintroduit.
        assertFalse(banking.contains("authorization-grant-type: client_credentials"));
        assertFalse(banking.contains("core-banking-client:"));
        assertFalse(banking.contains("CORE_BANKING_KEYSTORE_"));
        assertFalse(banking.contains("CORE_BANKING_TRUSTSTORE_"));
        assertFalse(banking.contains("CORE_BANKING_OAUTH2_"));

        // La configuration bancaire ne pilote pas l'authentification des utilisateurs.
        assertFalse(banking.contains("sixpay.security.authentication"));
        assertFalse(banking.contains("on-profile:"));
    }

    @Test
    void authenticationProfilesDoNotImportCustomerBankingCapability() throws Exception {
        for (String name : new String[]{
                "application-oidc.yml",
                "application-local-auth.yml",
                "application-hybrid-auth.yml"}) {
            assertFalse(read(name).contains("application-customer-banking.yml"), name);
        }
    }

    private static String read(String name) throws Exception {
        return Files.readString(RESOURCES.resolve(name));
    }
}
