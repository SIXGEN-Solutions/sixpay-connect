package com.sixpay.notification.infrastructure.operational.persistence;

import com.sixpay.notification.domain.model.NotificationAttempt;
import com.sixpay.notification.domain.model.NotificationChannel;
import com.sixpay.notification.domain.model.NotificationDeduplicationKey;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationIntent;
import com.sixpay.notification.domain.model.NotificationRecipient;
import com.sixpay.notification.domain.model.NotificationSourceReference;
import com.sixpay.notification.domain.model.NotificationTemplateKey;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;
import com.sixpay.notification.domain.repository.NotificationAttemptRepository;
import com.sixpay.notification.domain.repository.NotificationSaveResult;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OperationalNotificationPersistenceAdapter
        implements OperationalNotificationRepository,
        NotificationAttemptRepository {

    private final OperationalNotificationSpringDataRepository
            notificationRepository;

    private final OperationalNotificationAttemptSpringDataRepository
            attemptRepository;

    private final NotificationTemplateVariablesCodec variablesCodec;

    public OperationalNotificationPersistenceAdapter(
            OperationalNotificationSpringDataRepository notificationRepository,
            OperationalNotificationAttemptSpringDataRepository attemptRepository,
            NotificationTemplateVariablesCodec variablesCodec
    ) {
        this.notificationRepository = notificationRepository;
        this.attemptRepository = attemptRepository;
        this.variablesCodec = variablesCodec;
    }

    @Override
    @Transactional
    public NotificationSaveResult saveIfAbsent(
            OperationalNotificationDelivery delivery
    ) {
        var intent = delivery.intent();

        int inserted = notificationRepository.insertIfAbsent(
                intent.notificationId(),
                intent.source().triggerType().name(),
                intent.source().sourceId(),
                intent.recipient().type().name(),
                intent.recipient().reference(),
                intent.recipient().locale().toLanguageTag(),
                intent.channel().name(),
                intent.templateKey().name(),
                intent.deduplicationKey().value(),
                variablesCodec.encode(
                        intent.templateVariables()
                ),
                intent.createdAt(),
                intent.correlationId()
        );

        OperationalNotificationDelivery persisted =
                notificationRepository
                        .findByDeduplicationKey(
                                intent.deduplicationKey().value()
                        )
                        .map(this::toDomain)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Notification insert completed "
                                                + "without a readable row"
                                )
                        );

        return new NotificationSaveResult(
                persisted,
                inserted == 1
        );
    }

    @Override
    @Transactional
    public OperationalNotificationDelivery save(
            OperationalNotificationDelivery delivery
    ) {
        OperationalNotificationJpaEntity entity =
                notificationRepository.findById(
                                delivery.intent()
                                        .notificationId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Operational notification not found"
                                )
                        );

        entity.synchronize(
                delivery,
                variablesCodec.encode(
                        delivery.intent()
                                .templateVariables()
                )
        );

        return toDomain(
                notificationRepository.saveAndFlush(
                        entity
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OperationalNotificationDelivery> findById(
            UUID notificationId
    ) {
        return notificationRepository
                .findById(notificationId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OperationalNotificationDelivery>
    findByDeduplicationKey(
            NotificationDeduplicationKey deduplicationKey
    ) {
        return notificationRepository
                .findByDeduplicationKey(
                        deduplicationKey.value()
                )
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findDueNotificationIds(
            Instant dueAt,
            int limit
    ) {
        return notificationRepository
                .findDueNotificationIds(
                        dueAt,
                        limit
                );
    }

    @Override
    @Transactional
    public Optional<OperationalNotificationDelivery>
    claimForDispatch(
            UUID notificationId,
            Instant claimedAt
    ) {
        int claimed =
                notificationRepository.claimForDispatch(
                        notificationId,
                        claimedAt
                );

        if (claimed == 0) {
            return Optional.empty();
        }

        return notificationRepository
                .findById(notificationId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public NotificationAttempt append(
            NotificationAttempt attempt
    ) {
        OperationalNotificationAttemptJpaEntity entity =
                attemptRepository.findById(
                                attempt.attemptId()
                        )
                        .orElseGet(
                                () ->
                                        OperationalNotificationAttemptJpaEntity
                                                .from(attempt)
                        );

        entity.synchronize(attempt);

        return toDomain(
                attemptRepository.saveAndFlush(
                        entity
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationAttempt> findByNotificationId(
            UUID notificationId
    ) {
        return attemptRepository
                .findByNotificationIdOrderByAttemptNumberAsc(
                        notificationId
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private OperationalNotificationDelivery toDomain(
            OperationalNotificationJpaEntity entity
    ) {
        NotificationIntent intent =
                new NotificationIntent(
                        entity.notificationId(),
                        new NotificationSourceReference(
                                entity.triggerType(),
                                entity.sourceId()
                        ),
                        new NotificationRecipient(
                                entity.recipientType(),
                                entity.recipientReference(),
                                Locale.forLanguageTag(
                                        entity.recipientLocale()
                                )
                        ),
                        entity.channel(),
                        entity.templateKey(),
                        new NotificationDeduplicationKey(
                                entity.deduplicationKey()
                        ),
                        variablesCodec.decode(
                                entity.templateVariables()
                        ),
                        entity.status(),
                        entity.createdAt(),
                        entity.correlationId()
                );

        return new OperationalNotificationDelivery(
                intent,
                entity.attemptCount(),
                entity.nextAttemptAt(),
                entity.lastAttemptAt(),
                entity.deliveredAt(),
                entity.lastErrorCode(),
                entity.providerReference()
        );
    }

    private NotificationAttempt toDomain(
            OperationalNotificationAttemptJpaEntity entity
    ) {
        return new NotificationAttempt(
                entity.attemptId(),
                entity.notificationId(),
                entity.attemptNumber(),
                entity.startedAt(),
                entity.completedAt(),
                entity.outcome(),
                entity.errorCode()
        );
    }
}
