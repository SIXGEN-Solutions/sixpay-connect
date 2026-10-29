package com.sixpay.security.configuration;

import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;
import com.sixpay.security.authentication.SecurityContextCurrentMachineIdentityProvider;
import com.sixpay.security.infrastructure.authentication.machine.JpaPartnerMachineIdentityQueryAdapter;
import com.sixpay.security.infrastructure.authentication.machine.PartnerMachineIdentitySpringDataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PartnerMachineIdentityConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentMachineIdentityProvider.class)
    CurrentMachineIdentityProvider currentMachineIdentityProvider() {
        return new SecurityContextCurrentMachineIdentityProvider();
    }

    @Bean
    @ConditionalOnMissingBean(PartnerMachineIdentityQueryUseCase.class)
    PartnerMachineIdentityQueryUseCase partnerMachineIdentityQueryUseCase(
            PartnerMachineIdentitySpringDataRepository repository
    ) {
        return new JpaPartnerMachineIdentityQueryAdapter(repository);
    }
}
