package com.sixpay.bootstrap.configuration;

import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.payment.api.PaymentConfirmationApiMapper;
import com.sixpay.payment.api.PaymentConfirmationController;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.service.PaymentAuthorizationService;
import com.sixpay.payment.application.service.PaymentConfirmationRevocationService;
import com.sixpay.payment.application.service.PaymentConfirmationService;
import com.sixpay.payment.application.service.PaymentFinalizationService;
import com.sixpay.payment.application.service.PaymentPreConfirmationTerminationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the real @ConditionalOnProperty lifecycle, not just constructor calls.
 * The provider gateway is mocked: no bank request or external dependency.
 */
class PaymentConfirmationBeanActivationTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(
                        PaymentConfirmationService.class,
                        PaymentConfirmationRevocationService.class,
                        PaymentPreConfirmationTerminationService.class,
                        PaymentConfirmationController.class
                )
                .withBean(PaymentLookupPort.class, () -> mock(PaymentLookupPort.class))
                .withBean(PaymentAuthorizationService.class, () -> mock(PaymentAuthorizationService.class))
                .withBean(PaymentConfirmationIdempotencyPort.class, () -> mock(PaymentConfirmationIdempotencyPort.class))
                .withBean(PaymentConfirmationGateway.class, () -> mock(PaymentConfirmationGateway.class))
                .withBean(PaymentFinalizationService.class, () -> mock(PaymentFinalizationService.class))
                .withBean(PaymentConfirmationApiMapper.class, () -> mock(PaymentConfirmationApiMapper.class))
                .withBean(CorrelationIdResolver.class, CorrelationIdResolver::new);
    }

    @Test
    void enabledConfirmationRegistersAllFourBeansEvenWhenGatewayIsRegisteredLater() {
        runner()
                .withPropertyValues("sixpay.payment.banking.amplitude.confirmation.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(PaymentConfirmationService.class);
                    assertThat(context).hasSingleBean(PaymentConfirmationRevocationService.class);
                    assertThat(context).hasSingleBean(PaymentPreConfirmationTerminationService.class);
                    assertThat(context).hasSingleBean(PaymentConfirmationController.class);
                });
    }

    @Test
    void disabledConfirmationDoesNotRegisterAnyConfirmationEndpointOrService() {
        runner()
                .withPropertyValues("sixpay.payment.banking.amplitude.confirmation.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(PaymentConfirmationService.class);
                    assertThat(context).doesNotHaveBean(PaymentConfirmationRevocationService.class);
                    assertThat(context).doesNotHaveBean(PaymentPreConfirmationTerminationService.class);
                    assertThat(context).doesNotHaveBean(PaymentConfirmationController.class);
                });
    }

    @Test
    void enabledConfirmationFailsClosedWithoutBankingGateway() {
        new ApplicationContextRunner()
                .withUserConfiguration(PaymentConfirmationService.class)
                .withPropertyValues("sixpay.payment.banking.amplitude.confirmation.enabled=true")
                .withBean(PaymentLookupPort.class, () -> mock(PaymentLookupPort.class))
                .withBean(PaymentAuthorizationService.class, () -> mock(PaymentAuthorizationService.class))
                .withBean(PaymentConfirmationIdempotencyPort.class, () -> mock(PaymentConfirmationIdempotencyPort.class))
                .run(context -> assertThat(context).hasFailed());
    }
}
