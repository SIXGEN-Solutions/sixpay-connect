package com.sixpay.notification.domain.policy;

import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeduplicationKey;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationSourceReference;
import com.sixpay.notification.domain.model.NotificationTemplateKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

public final class NotificationDeduplicationKeyFactory {

    public NotificationDeduplicationKey create(
            NotificationSourceReference source,
            NotificationRecipient recipient,
            NotificationChannel channel,
            NotificationTemplateKey templateKey
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(templateKey, "templateKey");

        String canonical =
                source.triggerType().name()
                        + "|"
                        + source.sourceId()
                        + "|"
                        + recipient.type().name()
                        + "|"
                        + recipient.reference()
                        + "|"
                        + channel.name()
                        + "|"
                        + templateKey.name();

        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return new NotificationDeduplicationKey(
                    HexFormat.of()
                            .formatHex(digest)
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot create notification deduplication key",
                    exception
            );
        }
    }
}
