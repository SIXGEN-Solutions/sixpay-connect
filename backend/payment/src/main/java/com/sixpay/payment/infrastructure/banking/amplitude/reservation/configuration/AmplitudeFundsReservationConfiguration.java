package com.sixpay.payment.infrastructure.banking.amplitude.reservation.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.AmplitudeFundsReservationClient;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.client.RestAmplitudeFundsReservationClient;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.mapper.AmplitudeFundsReservationMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.validation.AmplitudeFundsReservationResponseValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        AmplitudeFundsReservationProperties.class
)
@ConditionalOnProperty(
        prefix = AmplitudeFundsReservationProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudeFundsReservationConfiguration {



    @Bean
    RestClient amplitudeFundsReservationRestClient(
            StandardRestClientFactory factory,
            AmplitudeFundsReservationProperties properties,
            SslBundles sslBundles
    ) {
        return factory.create(
                properties.baseUrl(),
                new HttpTimeoutPolicy(
                        properties.connectTimeout(),
                        properties.readTimeout()
                ),
                properties.security().mtlsEnabled()
                        ? sslBundles.getBundle(properties.security().sslBundle())
                                .createSslContext()
                        : null,
                List.of(),
                properties.security().allowInsecureHttp()
        );
    }

    @Bean
    AmplitudeFundsReservationMapper
    amplitudeFundsReservationMapper(
            ObjectMapper objectMapper
    ) {
        return new AmplitudeFundsReservationMapper(
                objectMapper,
                Clock.systemUTC()
        );
    }

    @Bean
    AmplitudeFundsReservationResponseValidator
    amplitudeFundsReservationResponseValidator(
            AmplitudeFundsReservationProperties properties
    ) {
        return new AmplitudeFundsReservationResponseValidator(
                properties
        );
    }

    @Bean
    AmplitudeFundsReservationClient
    amplitudeFundsReservationClient(
            RestClient amplitudeFundsReservationRestClient,

            AmplitudeFundsReservationProperties properties,
            AmplitudeFundsReservationMapper mapper,
            AmplitudeFundsReservationResponseValidator validator,
            ObjectMapper objectMapper
    ) {
        return new RestAmplitudeFundsReservationClient(
                amplitudeFundsReservationRestClient,
                properties,
                mapper,
                validator,
                objectMapper
        );
    }
}
