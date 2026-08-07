package com.sixpay.notification.infrastructure.operational.email;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguredSixPayAdminRecipientResolverTest {

    @Test
    void resolvesOnlyEnabledAdminRecipients() {
        var properties =
                new OperationalNotificationEmailProperties();

        properties.setAdminRecipients(
                List.of(
                        new OperationalNotificationEmailProperties
                                .AdminRecipient(
                                "operations-admin",
                                "operations@example.test",
                                "fr",
                                true
                        ),
                        new OperationalNotificationEmailProperties
                                .AdminRecipient(
                                "risk-admin",
                                "risk@example.test",
                                "fr",
                                false
                        )
                )
        );

        var resolver =
                new ConfiguredSixPayAdminRecipientResolver(
                        properties
                );

        var recipients =
                resolver.resolveActiveRecipients();

        assertEquals(1, recipients.size());
        assertEquals(
                "operations-admin",
                recipients.getFirst().reference()
        );
        assertEquals(
                "operations@example.test",
                resolver.resolveEmail(
                        "operations-admin"
                )
        );
    }

    @Test
    void unknownReferenceDoesNotExposeConfiguredAddresses() {
        var properties =
                new OperationalNotificationEmailProperties();

        properties.setAdminRecipients(
                List.of(
                        new OperationalNotificationEmailProperties
                                .AdminRecipient(
                                "operations-admin",
                                "operations@example.test",
                                "fr",
                                true
                        )
                )
        );

        var resolver =
                new ConfiguredSixPayAdminRecipientResolver(
                        properties
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolveEmail(
                                "unknown-admin"
                        )
                );

        org.junit.jupiter.api.Assertions.assertFalse(
                exception.getMessage().contains(
                        "operations@example.test"
                )
        );
    }

    @Test
    void rejectsDuplicateLogicalReferences() {
        var properties =
                new OperationalNotificationEmailProperties();

        properties.setAdminRecipients(
                List.of(
                        new OperationalNotificationEmailProperties
                                .AdminRecipient(
                                "operations-admin",
                                "first@example.test",
                                "fr",
                                true
                        ),
                        new OperationalNotificationEmailProperties
                                .AdminRecipient(
                                "operations-admin",
                                "second@example.test",
                                "fr",
                                true
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ConfiguredSixPayAdminRecipientResolver(
                        properties
                )
        );
    }
}
