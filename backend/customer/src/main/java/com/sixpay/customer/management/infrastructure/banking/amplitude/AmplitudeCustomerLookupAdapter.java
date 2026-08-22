package com.sixpay.customer.management.infrastructure.banking.amplitude;

import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnBean(AmplitudeCustomerLookupClient.class)
public final class AmplitudeCustomerLookupAdapter
        implements BankingCustomerLookupPort {

    private final AmplitudeCustomerLookupClient client;

    public AmplitudeCustomerLookupAdapter(
            AmplitudeCustomerLookupClient client
    ) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public BankingCustomerProfile lookup(
            BankingCustomerLookupQuery query
    ) {
        return client.lookup(query);
    }
}
