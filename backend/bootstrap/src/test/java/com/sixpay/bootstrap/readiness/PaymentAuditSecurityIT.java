package com.sixpay.bootstrap.readiness;

import com.sixpay.reporting.api.controller
        .PaymentAuditExportController;
import com.sixpay.reporting.api.controller
        .PaymentAuditQueryController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentAuditSecurityIT {

    @Test
    void auditReadOperationsRequireReadScope() {

        Arrays.stream(
                        PaymentAuditQueryController.class
                                .getDeclaredMethods()
                )
                .filter(method ->
                        java.lang.reflect.Modifier.isPublic(
                                method.getModifiers()
                        )
                )
                .filter(method ->
                        method.getAnnotation(
                                org.springframework.web.bind.annotation
                                        .GetMapping.class
                        ) != null
                )
                .forEach(method -> {
                    PreAuthorize auth =
                            method.getAnnotation(
                                    PreAuthorize.class
                            );
                    assertTrue(
                            auth != null
                                    && auth.value().contains(
                                    "SCOPE_payment.audit.read"
                            )
                    );
                });
    }

    @Test
    void exportOperationsRequireReadAndExportScopes() {

        Arrays.stream(
                        PaymentAuditExportController.class
                                .getDeclaredMethods()
                )
                .filter(method ->
                        java.lang.reflect.Modifier.isPublic(
                                method.getModifiers()
                        )
                )
                .filter(method ->
                        method.getAnnotation(
                                org.springframework.web.bind.annotation
                                        .PostMapping.class
                        ) != null
                                || method.getAnnotation(
                                org.springframework.web.bind.annotation
                                        .GetMapping.class
                        ) != null
                )
                .forEach(method -> {
                    PreAuthorize auth =
                            method.getAnnotation(
                                    PreAuthorize.class
                            );

                    assertTrue(auth != null);
                    assertTrue(auth.value().contains(
                            "SCOPE_payment.audit.read"
                    ));
                    assertTrue(auth.value().contains(
                            "SCOPE_payment.audit.export"
                    ));
                });
    }
}
