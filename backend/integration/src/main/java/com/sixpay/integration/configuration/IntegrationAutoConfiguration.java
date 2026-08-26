package com.sixpay.integration.configuration;

import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.integration.http.UuidRequestIdGenerator;
import com.sixpay.integration.messaging.json.IntegrationJsonSerializer;
import com.sixpay.integration.messaging.kafka.KafkaTopicNamingConvention;
import com.sixpay.integration.observability.IntegrationMetrics;
import com.sixpay.integration.observability.IntegrationObservation;
import com.sixpay.integration.resilience.RetryDecider;
import com.sixpay.integration.resilience.RetryPolicy;
import com.sixpay.integration.resilience.RetrySleeper;
import com.sixpay.integration.resilience.RetryingIntegrationExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class IntegrationAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    CorrelationIdResolver correlationIdResolver() { return new CorrelationIdResolver(); }

    @Bean @ConditionalOnMissingBean
    UuidRequestIdGenerator requestIdGenerator() { return new UuidRequestIdGenerator(); }

    @Bean @ConditionalOnMissingBean
    StandardRestClientFactory standardRestClientFactory(RestClient.Builder builder) {
        return new StandardRestClientFactory(builder);
    }

    @Bean @ConditionalOnMissingBean
    IntegrationJsonSerializer integrationJsonSerializer(ObjectMapper objectMapper) {
        return new IntegrationJsonSerializer(objectMapper);
    }

    @Bean @ConditionalOnMissingBean
    KafkaTopicNamingConvention kafkaTopicNamingConvention() {
        return new KafkaTopicNamingConvention();
    }

    @Bean @ConditionalOnMissingBean
    RetryingIntegrationExecutor retryingIntegrationExecutor() {
        return new RetryingIntegrationExecutor(
                RetryPolicy.DEFAULT,
                RetryDecider.safeDefault(),
                RetrySleeper.threadSleep()
        );
    }

    @Bean @ConditionalOnMissingBean
    IntegrationMetrics integrationMetrics(MeterRegistry registry) {
        return new IntegrationMetrics(registry);
    }

    @Bean @ConditionalOnMissingBean
    IntegrationObservation integrationObservation(ObservationRegistry registry) {
        return new IntegrationObservation(registry);
    }
}
