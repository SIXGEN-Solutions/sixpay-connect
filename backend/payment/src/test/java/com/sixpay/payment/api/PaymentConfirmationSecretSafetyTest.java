package com.sixpay.payment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sixpay.payment.api.request.VerifyPaymentConfirmationRequest;
import com.sixpay.payment.api.response.PaymentConfirmationResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentConfirmationSecretSafetyTest {

    @Test
    void verifyRequestKeepsOtpWriteOnlyRedactedAndDefensivelyCopied()
            throws Exception {
        char[] source = "123456".toCharArray();
        VerifyPaymentConfirmationRequest request =
                new VerifyPaymentConfirmationRequest(source);

        source[0] = '9';

        assertThat(request.toString())
                .doesNotContain("123456")
                .doesNotContain("923456")
                .contains("<redacted>");

        char[] firstRead = request.otp();
        assertThat(firstRead).containsExactly(
                '1', '2', '3', '4', '5', '6'
        );

        firstRead[0] = '8';
        assertThat(request.otp()).containsExactly(
                '1', '2', '3', '4', '5', '6'
        );

        Method accessor =
                VerifyPaymentConfirmationRequest.class.getMethod("otp");
        JsonProperty property = accessor.getAnnotation(JsonProperty.class);

        assertThat(property).isNotNull();
        assertThat(property.value()).isEqualTo("otp");
        assertThat(property.access())
                .isEqualTo(JsonProperty.Access.WRITE_ONLY);
    }

    @Test
    void verifyRequestDoesNotSerializeOtpBackToJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        VerifyPaymentConfirmationRequest request =
                new VerifyPaymentConfirmationRequest(
                        "654321".toCharArray()
                );

        String json = mapper.writeValueAsString(request);

        assertThat(json).isEqualTo("{}");
        assertThat(json)
                .doesNotContain("654321")
                .doesNotContain("otp");
    }

    @Test
    void publicResponseHasNoChallengeReferenceOrOtpField() {
        var fields = Arrays.stream(
                PaymentConfirmationResponse.class
                        .getRecordComponents()
        ).map(java.lang.reflect.RecordComponent::getName)
         .toList();

        assertThat(fields)
                .doesNotContain("challengeReference", "otp")
                .containsExactly(
                        "paymentReference",
                        "challengeStatus",
                        "businessCode",
                        "deliveryChannel",
                        "sentAt",
                        "expiresAt",
                        "verifiedAt"
                );
    }
}
