package com.sixpay.security.configuration;

import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.infrastructure.authentication.machine.PartnerMachineIdentitySpringDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PartnerMachineIdentityConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            PartnerMachineIdentityConfiguration.class
                    )
                    .withBean(
                            PartnerMachineIdentitySpringDataRepository.class,
                            () -> mock(
                                    PartnerMachineIdentitySpringDataRepository.class
                            )
                    );

    @Test
    void exposesMachineIdentityQueryUseCaseWhenRepositoryExists() {
        contextRunner.run(context ->
                assertThat(context)
                        .hasSingleBean(PartnerMachineIdentityQueryUseCase.class)
        );
    }
}
