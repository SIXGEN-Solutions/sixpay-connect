package com.sixpay.administration.infrastructure.monitoring;

import com.sixpay.administration.application.port.output.IntegrationHealthQueryPort;
import com.sixpay.administration.domain.model.IntegrationHealth;
import com.sixpay.administration.domain.model.IntegrationStatus;
import com.sixpay.common.time.TimeProvider;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class ActuatorIntegrationHealthQueryAdapter
        implements IntegrationHealthQueryPort {

    static final String INCLUDE_PROPERTY =
            "sixpay.administration.integration-health.include";

    private static final String METADATA_PREFIX =
            "sixpay.administration.integration-health.";

    private final HealthEndpoint healthEndpoint;
    private final Environment environment;
    private final TimeProvider timeProvider;

    public ActuatorIntegrationHealthQueryAdapter(
            HealthEndpoint healthEndpoint,
            Environment environment,
            TimeProvider timeProvider
    ) {
        this.healthEndpoint =
                Objects.requireNonNull(healthEndpoint);
        this.environment =
                Objects.requireNonNull(environment);
        this.timeProvider =
                Objects.requireNonNull(timeProvider);
    }

    @Override
    public List<IntegrationStatus> findAll() {
        Instant checkedAt = timeProvider.now();

        return includedIntegrationIds()
                .stream()
                .map(id -> status(id, checkedAt))
                .toList();
    }

    private List<String> includedIntegrationIds() {
        String configured = environment.getProperty(
                INCLUDE_PROPERTY,
                ""
        );

        if (configured.isBlank()) {
            return List.of();
        }

        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private IntegrationStatus status(
            String integrationId,
            Instant checkedAt
    ) {
        HealthDescriptor descriptor =
                healthEndpoint.healthForPath(
                        integrationId
                );

        String name = environment.getProperty(
                METADATA_PREFIX
                        + integrationId
                        + ".name",
                integrationId
        );

        String type = environment.getProperty(
                METADATA_PREFIX
                        + integrationId
                        + ".type",
                "RUNTIME_HEALTH"
        );

        if (descriptor == null) {
            return new IntegrationStatus(
                    integrationId,
                    name,
                    type,
                    IntegrationHealth.UNKNOWN,
                    "Health contributor is not registered",
                    null,
                    checkedAt
            );
        }

        Status status = descriptor.getStatus();

        return new IntegrationStatus(
                integrationId,
                name,
                type,
                map(status),
                safeDescription(status),
                null,
                checkedAt
        );
    }

    private static IntegrationHealth map(
            Status status
    ) {
        if (Status.UP.equals(status)) {
            return IntegrationHealth.AVAILABLE;
        }

        if (Status.DOWN.equals(status)
                || Status.OUT_OF_SERVICE.equals(status)) {
            return IntegrationHealth.UNAVAILABLE;
        }

        if (Status.UNKNOWN.equals(status)) {
            return IntegrationHealth.UNKNOWN;
        }

        return IntegrationHealth.DEGRADED;
    }

    private static String safeDescription(
            Status status
    ) {
        String description =
                status.getDescription();

        if (description == null
                || description.isBlank()) {
            return null;
        }

        return description;
    }
}
