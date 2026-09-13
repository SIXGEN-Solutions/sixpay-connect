package com.sixpay.administration.infrastructure.configuration;

import com.sixpay.administration.application.port.output.DynamicSettingEventPublisher;
import com.sixpay.administration.domain.model.DynamicSettingChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SpringDynamicSettingEventPublisher implements DynamicSettingEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringDynamicSettingEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishAfterCommit(DynamicSettingChanged event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publishEvent(event);
            }
        });
    }
}
