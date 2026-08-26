package com.sixpay.bootstrap.readiness;

import com.sixpay.customer.observation.api.controller
        .ObservedCustomerQueryController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQuerySecurityIT {

    @Test
    void everyPublicQueryOperationRequiresReadScope() {

        Arrays.stream(
                        ObservedCustomerQueryController.class
                                .getDeclaredMethods()
                )
                .filter(method ->
                        java.lang.reflect.Modifier.isPublic(
                                method.getModifiers()
                        )
                )
                .filter(method ->
                        !method.isSynthetic()
                )
                .forEach(
                        ObservedCustomerQuerySecurityIT
                                ::assertReadScope
                );
    }

    private static void assertReadScope(Method method) {
        PreAuthorize annotation =
                method.getAnnotation(PreAuthorize.class);

        assertTrue(
                annotation != null
                        && annotation.value().contains(
                        "SCOPE_observed-customer.read"
                ),
                () -> "Missing observed-customer.read on "
                        + method.getName()
        );
    }
}
