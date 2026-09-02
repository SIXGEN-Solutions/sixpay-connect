package com.sixpay.customer.verification.configuration;

import com.sixpay.customer.configuration.CustomerCapabilityUnavailableException;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.service.CustomerVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerVerificationApplicationConfigurationTest {

    @Test
    void wiresRealApplicationServiceWithoutAnyProfile() {
        try (var context =
                     new AnnotationConfigApplicationContext()) {

            context.register(
                    CustomerVerificationApplicationConfiguration.class
            );
            context.refresh();

            VerifyCustomerUseCase useCase =
                    context.getBean(
                            VerifyCustomerUseCase.class
                    );

            assertThat(useCase)
                    .isInstanceOf(
                            CustomerVerificationService.class
                    );

            assertThat(
                    context.getBean(
                            BankingCustomerVerificationPort.class
                    )
            ).isNotNull();

            assertThat(
                    context.getBean(
                            BankingCustomerLookupPort.class
                    )
            ).isNotNull();
        }
    }

    @Test
    void missingLookupInfrastructureFailsClosed() {
        try (var context =
                     new AnnotationConfigApplicationContext()) {

            context.register(
                    CustomerVerificationApplicationConfiguration.class
            );
            context.refresh();

            BankingCustomerLookupPort lookup =
                    context.getBean(
                            BankingCustomerLookupPort.class
                    );

            assertThatThrownBy(
                    () -> lookup.lookup(
                            new BankingCustomerLookupPort
                                    .BankingCustomerLookupQuery(
                                    "SIXPAY_BANK",
                                    "NIU-001",
                                    "CUSTOMER-001",
                                    "ACC-001",
                                    "corr-001"
                            )
                    )
            )
                    .isInstanceOf(
                            CustomerCapabilityUnavailableException.class
                    )
                    .hasMessageContaining(
                            "banking lookup"
                    );
        }
    }
}
