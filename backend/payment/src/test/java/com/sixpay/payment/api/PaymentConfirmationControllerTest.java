package com.sixpay.payment.api;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentConfirmationControllerTest {

    @Test
    void exposesOnlyApprovedPublicConfirmationRoutesAndScopes() {
        RequestMapping root =
                PaymentConfirmationController.class.getAnnotation(
                        RequestMapping.class
                );
        assertThat(root.value())
                .containsExactly(
                        "/v1/payments/{paymentReference}/confirmation-challenge"
                );

        assertScopeAndPost("create", "payment.confirmation.create", "");
        assertScopeAndPost("verify", "payment.confirmation.verify", "/verify");
        assertScopeAndPost("resend", "payment.confirmation.resend", "/resend");

        Method read = method("read");
        assertThat(read.getAnnotation(GetMapping.class)).isNotNull();
        assertThat(read.getAnnotation(PreAuthorize.class).value())
                .isEqualTo(
                        "hasAuthority('SCOPE_payment.confirmation.read')"
                );

        assertThat(
                Arrays.stream(
                        PaymentConfirmationController.class
                                .getDeclaredMethods()
                ).map(Method::getName)
        ).doesNotContain("revoke");
    }

    private static void assertScopeAndPost(
            String methodName,
            String scope,
            String expectedPath
    ) {
        Method method = method(methodName);
        PostMapping post = method.getAnnotation(PostMapping.class);
        assertThat(post).isNotNull();
        assertThat(post.value())
                .containsExactlyElementsOf(
                        expectedPath.isEmpty()
                                ? java.util.List.of()
                                : java.util.List.of(expectedPath)
                );
        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('SCOPE_" + scope + "')");
    }

    private static Method method(String name) {
        return Arrays.stream(
                PaymentConfirmationController.class.getDeclaredMethods()
        ).filter(m -> m.getName().equals(name))
         .findFirst()
         .orElseThrow();
    }
}
