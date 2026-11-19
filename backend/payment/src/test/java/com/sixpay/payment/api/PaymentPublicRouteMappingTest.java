package com.sixpay.payment.api;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPublicRouteMappingTest {
    @Test
    void fivePublicMappingsUseCanonicalPaths() throws Exception {
        assertThat(PaymentCommandController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/v1/payments");
        assertThat(PaymentConsultationController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/v1/payments");
        assertThat(PaymentConfirmationController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/v1/payments/{paymentReference}/confirmation-challenge");
        assertThat(java.util.Arrays.stream(PaymentCommandController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .findFirst().orElseThrow().getAnnotation(PostMapping.class).value())
                .containsExactly("/initiate");
        assertThat(PaymentConsultationController.class.getMethod("getPayment", String.class, String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/{paymentReference}");
        assertThat(java.util.Arrays.stream(PaymentConfirmationController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class)).count()).isEqualTo(1);
        assertThat(java.util.Arrays.stream(PaymentConfirmationController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .map(method -> method.getAnnotation(PostMapping.class).value()[0]).toList())
                .containsExactlyInAnyOrder("/verify", "/resend");
    }
}
