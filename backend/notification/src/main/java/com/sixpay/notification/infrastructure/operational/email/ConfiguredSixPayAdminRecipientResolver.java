package com.sixpay.notification.infrastructure.operational.email;

import com.sixpay.notification.application.port.output.AdminEmailAddressResolver;
import com.sixpay.notification.application.port.output.SixPayAdminRecipientResolver;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationRecipientType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConfiguredSixPayAdminRecipientResolver
        implements SixPayAdminRecipientResolver,
        AdminEmailAddressResolver {

    private final Map<
            String,
            OperationalNotificationEmailProperties.AdminRecipient
            > recipients;

    public ConfiguredSixPayAdminRecipientResolver(
            OperationalNotificationEmailProperties properties
    ) {
        Objects.requireNonNull(
                properties,
                "properties"
        );

        this.recipients =
                properties.getAdminRecipients()
                        .stream()
                        .filter(
                                OperationalNotificationEmailProperties
                                        .AdminRecipient::enabled
                        )
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        OperationalNotificationEmailProperties
                                                .AdminRecipient::reference,
                                        Function.identity(),
                                        (left, right) -> {
                                            throw new IllegalArgumentException(
                                                    "Duplicate admin recipient reference: "
                                                            + left.reference()
                                            );
                                        }
                                )
                        );
    }

    @Override
    public List<NotificationRecipient>
    resolveActiveRecipients() {
        return recipients.values()
                .stream()
                .map(recipient ->
                        new NotificationRecipient(
                                NotificationRecipientType
                                        .SIXPAY_ADMIN,
                                recipient.reference(),
                                Locale.forLanguageTag(
                                        recipient.locale()
                                )
                        )
                )
                .sorted(
                        java.util.Comparator.comparing(
                                NotificationRecipient::reference
                        )
                )
                .toList();
    }

    @Override
    public String resolveEmail(
            String recipientReference
    ) {
        if (recipientReference == null
                || recipientReference.isBlank()) {
            throw new IllegalArgumentException(
                    "recipientReference is required"
            );
        }

        var recipient =
                recipients.get(
                        recipientReference.strip()
                );

        if (recipient == null) {
            throw new IllegalArgumentException(
                    "Unknown SIXPAY admin recipient reference"
            );
        }

        return recipient.email();
    }
}
