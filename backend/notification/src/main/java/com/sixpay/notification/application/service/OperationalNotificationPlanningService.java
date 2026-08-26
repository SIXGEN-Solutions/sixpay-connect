package com.sixpay.notification.application.service;

import com.sixpay.notification.application.port.input.OperationalNotificationTriggerUseCase;
import com.sixpay.notification.application.port.output.NotificationIdGenerator;
import com.sixpay.notification.application.port.output.SixPayAdminRecipientResolver;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationIntent;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationRecipientType;
import com.sixpay.notification.domain.model.OperationalNotificationTrigger;
import com.sixpay.notification.domain.policy.NotificationDeduplicationKeyFactory;
import com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog;
import com.sixpay.notification.domain.policy.OperationalNotificationRoutingPolicy;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OperationalNotificationPlanningService
        implements OperationalNotificationTriggerUseCase {

    private final SixPayAdminRecipientResolver recipientResolver;
    private final NotificationIdGenerator idGenerator;
    private final OperationalNotificationRoutingPolicy routingPolicy;
    private final NotificationDeduplicationKeyFactory deduplicationKeyFactory;
    private final NotificationTemplateVariableMapper variableMapper;
    private final OperationalNotificationTemplateCatalog templateCatalog;
    private final Clock clock;

    public OperationalNotificationPlanningService(
            SixPayAdminRecipientResolver recipientResolver,
            NotificationIdGenerator idGenerator,
            OperationalNotificationRoutingPolicy routingPolicy,
            NotificationDeduplicationKeyFactory deduplicationKeyFactory,
            NotificationTemplateVariableMapper variableMapper,
            OperationalNotificationTemplateCatalog templateCatalog,
            Clock clock
    ) {
        this.recipientResolver = Objects.requireNonNull(
                recipientResolver,
                "recipientResolver"
        );
        this.idGenerator = Objects.requireNonNull(
                idGenerator,
                "idGenerator"
        );
        this.routingPolicy = Objects.requireNonNull(
                routingPolicy,
                "routingPolicy"
        );
        this.deduplicationKeyFactory = Objects.requireNonNull(
                deduplicationKeyFactory,
                "deduplicationKeyFactory"
        );
        this.variableMapper = Objects.requireNonNull(
                variableMapper,
                "variableMapper"
        );
        this.templateCatalog = Objects.requireNonNull(
                templateCatalog,
                "templateCatalog"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock"
        );
    }

    @Override
    public List<NotificationIntent> plan(
            OperationalNotificationTrigger trigger
    ) {
        Objects.requireNonNull(
                trigger,
                "trigger"
        );

        var route = routingPolicy.route(trigger);
        Map<String, String> variables =
                variableMapper.map(trigger);

        var templateDefinition =
                templateCatalog.definition(
                        route.templateKey()
                );

        if (!templateDefinition
                .allowedVariables()
                .containsAll(
                        variables.keySet()
                )) {
            throw new IllegalStateException(
                    "Template variables exceed the "
                            + "approved allow-list"
            );
        }

        return recipientResolver
                .resolveActiveRecipients()
                .stream()
                .filter(recipient ->
                        recipient.type()
                                == NotificationRecipientType
                                .SIXPAY_ADMIN
                )
                .map(recipient ->
                        toIntent(
                                trigger,
                                recipient,
                                route,
                                variables
                        )
                )
                .toList();
    }

    private NotificationIntent toIntent(
            OperationalNotificationTrigger trigger,
            NotificationRecipient recipient,
            OperationalNotificationRoutingPolicy
                    .NotificationRoute route,
            Map<String, String> variables
    ) {
        return new NotificationIntent(
                idGenerator.nextId(),
                trigger.sourceReference(),
                recipient,
                route.channel(),
                route.templateKey(),
                deduplicationKeyFactory.create(
                        trigger.sourceReference(),
                        recipient,
                        route.channel(),
                        route.templateKey()
                ),
                variables,
                NotificationDeliveryStatus.PENDING,
                clock.instant(),
                trigger.correlationId()
        );
    }
}
