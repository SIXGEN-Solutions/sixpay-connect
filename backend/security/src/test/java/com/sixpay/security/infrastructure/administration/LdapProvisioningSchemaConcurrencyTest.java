package com.sixpay.security.infrastructure.administration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LdapProvisioningSchemaConcurrencyTest {

    @Test
    void schemaEnforcesOneCanonicalLinkPerLdapIdentity() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/"
                                        + "V700__security_baseline.sql"
                        ),
                        StandardCharsets.UTF_8
                );

        assertThat(sql)
                .contains(
                        "CONSTRAINT uk_security_user_identity_provider_subject",
                        "UNIQUE (identity_type, provider, provider_subject)"
                );
    }

    @Test
    void schemaPreventsDuplicateCanonicalUsername() throws Exception {
        String sql =
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/"
                                        + "V700__security_baseline.sql"
                        ),
                        StandardCharsets.UTF_8
                );

        assertThat(sql)
                .contains(
                        "uk_security_user_accounts_normalized_username",
                        "UNIQUE (normalized_username)"
                );
    }
}
