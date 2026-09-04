package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.api.request.VerifyPaymentConfirmationRequest;
import com.sixpay.payment.application.port.input.CreatePaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ReadPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ResendPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.VerifyPaymentConfirmationUseCase;
import com.sixpay.payment.application.view.PaymentConfirmationView;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentConfirmationHttpContractTest {

    private static final String PAYMENT_REFERENCE =
            "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String CORRELATION_ID =
            "11111111-1111-4111-8111-111111111111";

    @Test
    void mutationResponsesExposeReplayMetadataAndReadDoesNot() {
        CreatePaymentConfirmationUseCase createUseCase =
                mock(CreatePaymentConfirmationUseCase.class);
        ReadPaymentConfirmationUseCase readUseCase =
                mock(ReadPaymentConfirmationUseCase.class);
        VerifyPaymentConfirmationUseCase verifyUseCase =
                mock(VerifyPaymentConfirmationUseCase.class);
        ResendPaymentConfirmationUseCase resendUseCase =
                mock(ResendPaymentConfirmationUseCase.class);
        CorrelationIdResolver resolver = mock(CorrelationIdResolver.class);

        CorrelationId correlationId = CorrelationId.of(CORRELATION_ID);
        when(resolver.resolve(null)).thenReturn(correlationId);
        when(createUseCase.create(any())).thenReturn(view(false));
        when(readUseCase.read(any())).thenReturn(view(false));
        when(verifyUseCase.verify(any())).thenReturn(view(true));
        when(resendUseCase.resend(any())).thenReturn(view(true));

        PaymentConfirmationController controller =
                new PaymentConfirmationController(
                        createUseCase,
                        readUseCase,
                        verifyUseCase,
                        resendUseCase,
                        new PaymentConfirmationApiMapper(),
                        resolver
                );

        ResponseEntity<?> create = controller.create(
                PAYMENT_REFERENCE,
                "create-key-0001",
                null
        );
        assertReplayHeader(create, "false");

        ResponseEntity<?> read = controller.read(PAYMENT_REFERENCE, null);
        assertThat(
                read.getHeaders().getFirst("Idempotency-Replayed")
        ).isNull();

        ResponseEntity<?> verify = controller.verify(
                PAYMENT_REFERENCE,
                new VerifyPaymentConfirmationRequest("123456".toCharArray()),
                "verify-key-0001",
                null
        );
        assertReplayHeader(verify, "true");

        ResponseEntity<?> resend = controller.resend(
                PAYMENT_REFERENCE,
                "resend-key-0001",
                null
        );
        assertReplayHeader(resend, "true");
    }

    private static void assertReplayHeader(
            ResponseEntity<?> response,
            String expected
    ) {
        assertThat(
                response.getHeaders().getFirst(
                        IntegrationHttpHeaders.CORRELATION_ID
                )
        ).isEqualTo(CORRELATION_ID);

        assertThat(
                response.getHeaders().getFirst("Idempotency-Replayed")
        ).isEqualTo(expected);
    }

    private static PaymentConfirmationView view(boolean replayed) {
        return new PaymentConfirmationView(
                PublicPaymentReference.of(PAYMENT_REFERENCE),
                ConfirmationChallengeStatus.ACTIVE,
                ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                null,
                null,
                null,
                null,
                replayed
        );
    }
}
