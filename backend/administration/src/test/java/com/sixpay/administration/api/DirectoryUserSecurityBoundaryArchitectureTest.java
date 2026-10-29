package com.sixpay.administration.api;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DirectoryUserSecurityBoundaryArchitectureTest {

    @Test
    void administrationUsesOnlyReviewedSecurityPublicSurfacesForDirectoryAdministration()
            throws Exception {

        String controller =
                Files.readString(
                        Path.of(
                                "src/main/java/com/sixpay/administration/api/"
                                        + "DirectoryUserAdministrationController.java"
                        ),
                        StandardCharsets.UTF_8
                );

        String exceptionHandler =
                Files.readString(
                        Path.of(
                                "src/main/java/com/sixpay/administration/api/"
                                        + "DirectoryUserApiExceptionHandler.java"
                        ),
                        StandardCharsets.UTF_8
                );

        String combined = String.join(System.lineSeparator(), controller, exceptionHandler);

        assertThat(combined)
                .contains(
                        "com.sixpay.security.application.port.input",
                        "com.sixpay.security.application.model",
                        "com.sixpay.security.authentication.CurrentUserProvider"
                )
                .doesNotContain(
                        "com.sixpay.security.infrastructure",
                        "org.springframework.ldap",
                        "javax.naming",
                        "LdapTemplate",
                        "LdapContextSource"
                );
    }
}
