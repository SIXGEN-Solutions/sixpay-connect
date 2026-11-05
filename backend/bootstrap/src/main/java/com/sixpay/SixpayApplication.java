package com.sixpay;

import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort;
import com.sixpay.payment.application.service.PaymentEventLifecycleOrchestrationService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class SixpayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SixpayApplication.class, args);
    }

    @Bean
    ApplicationRunner diagnosePostingBeans(
            ApplicationContext context,
            Environment environment
    ) {
        return args -> {
            System.out.println(
                    "Posting enabled: " +
                            environment.getProperty(
                                    "sixpay.payment.banking.amplitude.posting.enabled"
                            )
            );

            System.out.println(
                    "Execution port: " +
                            Arrays.toString(context.getBeanNamesForType(
                                    PaymentEventExecutionPort.class
                            ))
            );

            System.out.println(
                    "Recovery port: " +
                            Arrays.toString(context.getBeanNamesForType(
                                    PaymentEventRecoveryPort.class
                            ))
            );

            System.out.println(
                    "Lifecycle service: " +
                            Arrays.toString(context.getBeanNamesForType(
                                    PaymentEventLifecycleOrchestrationService.class
                            ))
            );
        };
    }

}
