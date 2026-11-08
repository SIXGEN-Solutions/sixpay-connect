package com.sixpay.payment.api;

import com.sixpay.payment.application.exception.PaymentConfirmationStateConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentConfirmationStateConflictResponseTest {

    @Test
    void incompatibleOtpVerificationStateProducesSafeConflict() {
        var request = new MockHttpServletRequest("POST",
                "/v1/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV/confirmation-challenge/verify");
        request.addHeader("X-Correlation-ID", "11111111-1111-1111-1111-111111111111");

        var response = new PaymentApiExceptionHandler()
                .paymentConfirmationStateConflict(
                        new PaymentConfirmationStateConflictException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getHeaders().getFirst("X-Correlation-ID"))
                .isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("PAYMENT_CONFIRMATION_STATE_CONFLICT");
        assertThat(response.getBody().detail())
                .isEqualTo("Payment is not awaiting OTP confirmation");
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().detail()).doesNotContain("IllegalStateException");
    }
}
