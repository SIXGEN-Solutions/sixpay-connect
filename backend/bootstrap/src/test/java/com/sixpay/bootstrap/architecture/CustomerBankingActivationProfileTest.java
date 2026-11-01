package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerBankingActivationProfileTest {
    private static final Path ROOT = Path.of("src/main/resources");

    @Test
    void bankingCapabilityCanBeEnabledIndependentlyOfEnvironmentAndUserIdentity() throws Exception {
        String source = Files.readString(ROOT.resolve("application-customer-banking-enabled.yml"));
        assertThat(source).contains("on-profile: customer-banking-enabled");
        assertThat(source).contains("classpath:application-customer-banking.yml");
        assertThat(source).doesNotContain("local-auth", "hybrid-auth", "oidc-common", "ldap-common");
        for (String environment : new String[]{"application-preproduction.yml", "application-production.yml"}) {
            String env = Files.readString(ROOT.resolve(environment));
            assertThat(env).doesNotContain("classpath:application-customer-banking.yml");
        }
    }

    @Test
    void bankingActivationSupportsExplicitDisableIncludingSandbox() throws Exception {
        String banking = Files.readString(ROOT.resolve("application-customer-banking.yml"));
        String sandbox = Files.readString(ROOT.resolve("application-amplitude-sandbox.yml"));
        assertThat(banking).contains("enabled: ${CUSTOMER_VERIFICATION_BANKING_ENABLED:true}");
        assertThat(sandbox).contains("enabled: ${CUSTOMER_VERIFICATION_BANKING_ENABLED:true}");
    }
}
