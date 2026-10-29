package com.sixpay.security.infrastructure.authentication.ldap;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class LdapSensitiveLoggingArchitectureTest {
    @Test
    void ldapAdapterDoesNotLogDirectoryCredentialsOrIdentifiers() throws Exception {
        Path file=Path.of("src/main/java/com/sixpay/security/infrastructure/authentication/ldap/ActiveDirectoryLdapAuthenticationAdapter.java");
        assertTrue(Files.isRegularFile(file));
        String source=Files.readString(file).toLowerCase(Locale.ROOT);
        assertFalse(source.contains("logger."));
        assertFalse(source.contains("log."));
        assertFalse(source.contains("system.out"));
        assertFalse(source.contains("system.err"));
    }
}
