package com.sixpay.bootstrap.configuration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministrationIncidentSecurityArchitectureTest {

    private static final Path ADMINISTRATION_CONTROLLER =
            Path.of(
                    "../administration/src/main/java/com/sixpay/"
                            + "administration/api/"
                            + "AdministrationQueryController.java"
            );

    private static final Path INCIDENT_CONTROLLER =
            Path.of(
                    "../administration/src/main/java/com/sixpay/"
                            + "administration/api/"
                            + "IncidentQueryController.java"
            );

    private static final Path ADMINISTRATION_ROUTES =
            Path.of(
                    "../../frontend/src/app/features/"
                            + "administration/administration.routes.ts"
            );

    private static final Path INCIDENT_ROUTES =
            Path.of(
                    "../../frontend/src/app/features/"
                            + "incidents/incidents.routes.ts"
            );

    @Test
    void administrationUsesAdminRoleOnBackendAndAngular()
            throws Exception {

        String backend =
                Files.readString(
                        ADMINISTRATION_CONTROLLER
                );

        String frontend =
                Files.readString(
                        ADMINISTRATION_ROUTES
                );

        assertTrue(
                backend.contains(
                        "@PreAuthorize(\"hasRole('ADMIN')\")"
                )
        );

        assertTrue(
                frontend.contains(
                        "const ADMIN_ONLY = ['ADMIN'] as const;"
                )
        );

        assertFalse(
                backend.contains(
                        "SCOPE_administration.read"
                )
        );

        assertFalse(
                frontend.contains(
                        "administration.read"
                )
        );
    }

    @Test
    void incidentsUseSameReadRolesOnBackendAndAngular()
            throws Exception {

        String backend =
                Files.readString(
                        INCIDENT_CONTROLLER
                );

        String frontend =
                Files.readString(
                        INCIDENT_ROUTES
                );

        assertTrue(
                backend.contains(
                        "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')"
                )
        );

        assertTrue(
                frontend.contains(
                        "const INCIDENT_READ_ROLES = "
                                + "['ADMIN', 'MANAGER', 'AUDITOR'] "
                                + "as const;"
                )
        );

        assertFalse(
                backend.contains(
                        "SCOPE_incident.read"
                )
        );

        assertFalse(
                frontend.contains(
                        "incident.read"
                )
        );
    }
}
