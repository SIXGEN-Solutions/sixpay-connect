package com.sixpay.accounting.configuration;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.service.AccountingCandidateProjectionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class AccountingCandidateProjectionConfiguration {
    @Bean
    AccountingCandidateProjectionService accountingCandidateProjectionService(
            AccountingCandidateProjectionRepository repository
    ) {
        return new AccountingCandidateProjectionService(repository, Clock.systemUTC());
    }
}
