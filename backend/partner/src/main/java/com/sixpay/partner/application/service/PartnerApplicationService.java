package com.sixpay.partner.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.partner.application.command.ConfigureValidationThresholdCommand;
import com.sixpay.partner.application.command.CreatePartnerCommand;
import com.sixpay.partner.application.command.DecidePartnerCommand;
import com.sixpay.partner.application.command.PartnerDecision;
import com.sixpay.partner.application.command.ReactivatePartnerCommand;
import com.sixpay.partner.application.command.SuspendPartnerCommand;
import com.sixpay.partner.application.exception.PartnerNotFoundException;
import com.sixpay.partner.application.port.input.PartnerManagementUseCase;
import com.sixpay.partner.application.port.input.PartnerQueryUseCase;
import com.sixpay.partner.application.port.output.PartnerAuditRecord;
import com.sixpay.partner.application.port.output.PartnerAuditTrail;
import com.sixpay.partner.application.port.output.PartnerEventPublisher;
import com.sixpay.partner.application.port.output.PartnerIdempotencyStore;
import com.sixpay.partner.application.port.output.PartnerOperationMetrics;
import com.sixpay.partner.application.port.output.PartnerThresholdHistory;
import com.sixpay.partner.application.port.output.PartnerThresholdHistoryRecord;
import com.sixpay.partner.application.view.PartnerAuditPage;
import com.sixpay.partner.application.view.PartnerAuditView;
import com.sixpay.partner.application.view.PartnerView;
import com.sixpay.partner.domain.event.PartnerCreated;
import com.sixpay.partner.domain.event.PartnerDomainEvent;
import com.sixpay.partner.domain.event.PartnerStatusChanged;
import com.sixpay.partner.domain.event.PartnerThresholdConfigured;
import com.sixpay.partner.domain.model.AuthorizedPerimeter;
import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerName;
import com.sixpay.partner.domain.model.TechnicalContact;
import com.sixpay.partner.domain.model.ValidationThreshold;
import com.sixpay.partner.domain.repository.PartnerRepository;
import com.sixpay.partner.events.PartnerCreatedIntegrationEvent;
import com.sixpay.partner.events.PartnerIntegrationEvent;
import com.sixpay.partner.events.PartnerStatusChangedIntegrationEvent;
import com.sixpay.partner.events.PartnerThresholdConfiguredIntegrationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class PartnerApplicationService implements PartnerManagementUseCase, PartnerQueryUseCase {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PartnerApplicationService.class);

    private final PartnerRepository partnerRepository;
    private final IdentifierGenerator<UUID> identifierGenerator;
    private final PartnerEventPublisher eventPublisher;
    private final PartnerAuditTrail auditTrail;
    private final PartnerThresholdHistory thresholdHistory;
    private final PartnerIdempotencyStore idempotencyStore;
    private final PartnerOperationMetrics metrics;
    private final TimeProvider timeProvider;

    public PartnerApplicationService(
            PartnerRepository partnerRepository,
            IdentifierGenerator<UUID> identifierGenerator,
            PartnerEventPublisher eventPublisher,
            PartnerAuditTrail auditTrail,
            PartnerThresholdHistory thresholdHistory,
            PartnerIdempotencyStore idempotencyStore,
            PartnerOperationMetrics metrics,
            TimeProvider timeProvider
    ) {
        this.partnerRepository = Objects.requireNonNull(partnerRepository);
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.auditTrail = Objects.requireNonNull(auditTrail);
        this.thresholdHistory = Objects.requireNonNull(thresholdHistory);
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore);
        this.metrics = Objects.requireNonNull(metrics);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public PartnerView create(CreatePartnerCommand command) {
        Objects.requireNonNull(command, "command is required");
        var operation = "PARTNER_CREATE";
        var replay = replay(operation, command.idempotencyKey(), PartnerOperationMetrics.Operation.CREATE);
        if (replay != null) {
            return replay;
        }
        var now = timeProvider.now();
        var partner = Partner.create(
                new PartnerId(identifierGenerator.generate()),
                new PartnerName(command.legalName()),
                new TechnicalContact(command.technicalContactName(), command.technicalContactEmail()),
                AuthorizedPerimeter.of(command.authorizedTransactionTypes()),
                now
        );
        persistAndPublish(partner, command.actorId(), correlation(command.correlationId()));
        appendAudit(partner.id(), "PARTNER_CREATED", command.actorId(), correlation(command.correlationId()),
                "Partner created input PENDING_VALIDATION status", now);
        idempotencyStore.complete(operation, requireIdempotencyKey(command.idempotencyKey()), partner.id(), now);
        metrics.succeeded(PartnerOperationMetrics.Operation.CREATE);
        logOutcome("create", partner, correlation(command.correlationId()));
        return PartnerView.from(partner);
    }

    @Override
    public PartnerView decide(DecidePartnerCommand command) {
        Objects.requireNonNull(command, "command is required");
        var operation = operation("PARTNER_DECIDE", command.partnerId());
        var replay = replay(operation, command.idempotencyKey(), PartnerOperationMetrics.Operation.DECIDE);
        if (replay != null) {
            return replay;
        }
        var partner = load(command.partnerId());
        var now = timeProvider.now();
        var decision = Objects.requireNonNull(command.decision(), "decision is required");
        if (decision == PartnerDecision.APPROVE) {
            partner.approve(now);
        } else {
            partner.reject(command.reason(), now);
        }

        persistAndPublish(partner, command.actorId(), correlation(command.correlationId()));

        String auditAction = switch (decision) {
            case APPROVE -> "PARTNER_APPROVED";
            case REJECT -> "PARTNER_REJECTED";
        };

        appendAudit(partner.id(), auditAction,
                command.actorId(), correlation(command.correlationId()),
                "Decision applied; status=" + partner.status(), now);
        idempotencyStore.complete(operation, requireIdempotencyKey(command.idempotencyKey()), partner.id(), now);
        metrics.succeeded(PartnerOperationMetrics.Operation.DECIDE);
        logOutcome("decide", partner, correlation(command.correlationId()));
        return PartnerView.from(partner);
    }

    @Override
    public PartnerView suspend(SuspendPartnerCommand command) {
        Objects.requireNonNull(command, "command is required");
        var operation = operation("PARTNER_SUSPEND", command.partnerId());
        var replay = replay(operation, command.idempotencyKey(), PartnerOperationMetrics.Operation.SUSPEND);
        if (replay != null) {
            return replay;
        }
        var partner = load(command.partnerId());
        var now = timeProvider.now();
        partner.suspend(command.reason(), now);
        persistAndPublish(partner, command.actorId(), correlation(command.correlationId()));
        appendAudit(partner.id(), "PARTNER_SUSPENDED", command.actorId(), correlation(command.correlationId()),
                "Partner suspended; reason=" + partner.statusReason().orElseThrow(), now);
        idempotencyStore.complete(operation, requireIdempotencyKey(command.idempotencyKey()), partner.id(), now);
        metrics.succeeded(PartnerOperationMetrics.Operation.SUSPEND);
        logOutcome("suspend", partner, correlation(command.correlationId()));
        return PartnerView.from(partner);
    }

    @Override
    public PartnerView reactivate(ReactivatePartnerCommand command) {
        Objects.requireNonNull(command, "command is required");
        var operation = operation("PARTNER_REACTIVATE", command.partnerId());
        var replay = replay(operation, command.idempotencyKey(), PartnerOperationMetrics.Operation.REACTIVATE);
        if (replay != null) {
            return replay;
        }
        var partner = load(command.partnerId());
        var now = timeProvider.now();
        partner.reactivate(now);
        persistAndPublish(partner, command.actorId(), correlation(command.correlationId()));
        appendAudit(partner.id(), "PARTNER_REACTIVATED", command.actorId(), correlation(command.correlationId()),
                "Partner reactivated", now);
        idempotencyStore.complete(operation, requireIdempotencyKey(command.idempotencyKey()), partner.id(), now);
        metrics.succeeded(PartnerOperationMetrics.Operation.REACTIVATE);
        logOutcome("reactivate", partner, correlation(command.correlationId()));
        return PartnerView.from(partner);
    }

    @Override
    public PartnerView configureValidationThreshold(ConfigureValidationThresholdCommand command) {
        Objects.requireNonNull(command, "command is required");
        var current = new ValidationThreshold(
                command.transactionType(),
                command.currency(),
                command.amount(),
                command.validationLevels()
        );
        var operation = operation(
                "PARTNER_THRESHOLD_" + current.transactionType() + "_" + current.currency(),
                command.partnerId()
        );
        var replay = replay(
                operation,
                command.idempotencyKey(),
                PartnerOperationMetrics.Operation.CONFIGURE_THRESHOLD
        );
        if (replay != null) {
            return replay;
        }
        var partner = load(command.partnerId());
        var previous = partner.thresholdFor(command.transactionType(), command.currency()).orElse(null);
        var now = timeProvider.now();
        partner.configureValidationThreshold(current, now);
        persistAndPublish(partner, command.actorId(), correlation(command.correlationId()));
        thresholdHistory.append(new PartnerThresholdHistoryRecord(
                partner.id(),
                previous,
                current,
                command.actorId(),
                correlation(command.correlationId()),
                now
        ));
        appendAudit(partner.id(), "VALIDATION_THRESHOLD_CONFIGURED",
                command.actorId(), correlation(command.correlationId()),
                "Threshold configured for transactionType=" + current.transactionType()
                        + ", currency=" + current.currency(), now);
        idempotencyStore.complete(operation, requireIdempotencyKey(command.idempotencyKey()), partner.id(), now);
        metrics.succeeded(PartnerOperationMetrics.Operation.CONFIGURE_THRESHOLD);
        logOutcome(
                "configure_threshold",
                partner,
                correlation(command.correlationId())
        );
        return PartnerView.from(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerView findById(PartnerId partnerId) {
        return PartnerView.from(load(partnerId));
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerAuditPage findAuditTrail(
            PartnerId partnerId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(from, "from is required");
        Objects.requireNonNull(to, "to is required");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or positive");
        }
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }
        var result = auditTrail.findByPartnerIdAndPeriod(partnerId, from, to, page, size);
        var items = result.records().stream()
                .map(PartnerAuditView::from)
                .toList();
        var totalPages = result.totalElements() == 0
                ? 0
                : ((result.totalElements() - 1) / size) + 1;
        return new PartnerAuditPage(items, page, size, result.totalElements(), totalPages);
    }

    private Partner load(PartnerId partnerId) {
        Objects.requireNonNull(partnerId, "partnerId is required");
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
    }

    private PartnerView replay(
            String operation,
            String idempotencyKey,
            PartnerOperationMetrics.Operation metricOperation
    ) {
        var normalizedKey = requireIdempotencyKey(idempotencyKey);
        idempotencyStore.lock(operation, normalizedKey);
        var replay = idempotencyStore.findCompleted(operation, normalizedKey)
                .map(this::load)
                .map(PartnerView::from)
                .orElse(null);
        if (replay != null) {
            metrics.replayed(metricOperation);
        }
        return replay;
    }

    private static String operation(String operation, PartnerId partnerId) {
        return operation + ":" + partnerId;
    }

    private static String requireIdempotencyKey(String value) {
        var key = requireText(value, "idempotencyKey");
        if (key.length() > 150) {
            throw new IllegalArgumentException("idempotencyKey must not exceed 150 characters");
        }
        return key;
    }

    private void persistAndPublish(Partner partner, String actorId, String correlationId) {
        var events = partner.pullDomainEvents();
        partnerRepository.save(partner);
        events.stream()
                .map(event -> toIntegrationEvent(event, partner, actorId, correlationId))
                .forEach(eventPublisher::publish);
    }

    private PartnerIntegrationEvent toIntegrationEvent(
            PartnerDomainEvent event,
            Partner partner,
            String actorId,
            String correlationId
    ) {
        if (event instanceof PartnerCreated created) {
            return new PartnerCreatedIntegrationEvent(
                    1,
                    created.eventId(),
                    created.partnerId().value(),
                    partner.legalName().value(),
                    partner.technicalContact().email(),
                    requireText(actorId, "actorId"),
                    requireText(correlationId, "correlationId"),
                    created.occurredAt()
            );
        }
        if (event instanceof PartnerStatusChanged changed) {
            return new PartnerStatusChangedIntegrationEvent(
                    2,
                    changed.eventId(),
                    changed.partnerId().value(),
                    changed.previousStatus(),
                    changed.currentStatus(),
                    changed.reason(),
                    partner.technicalContact().email(),
                    requireText(actorId, "actorId"),
                    requireText(correlationId, "correlationId"),
                    changed.occurredAt()
            );
        }
        if (event instanceof PartnerThresholdConfigured configured) {
            return new PartnerThresholdConfiguredIntegrationEvent(
                    1,
                    configured.eventId(),
                    configured.partnerId().value(),
                    configured.threshold().transactionType(),
                    configured.threshold().currency(),
                    configured.threshold().amount(),
                    configured.threshold().validationLevels(),
                    requireText(actorId, "actorId"),
                    requireText(correlationId, "correlationId"),
                    configured.occurredAt()
            );
        }
        throw new IllegalArgumentException("unsupported domain event: " + event.getClass().getName());
    }

    private void appendAudit(
            PartnerId partnerId,
            String action,
            String actorId,
            String correlationId,
            String details,
            Instant occurredAt
    ) {
        auditTrail.append(new PartnerAuditRecord(
                partnerId,
                action,
                "SUCCESS",
                requireText(actorId, "actorId"),
                requireText(correlationId, "correlationId"),
                details,
                occurredAt
        ));
    }

    private static void logOutcome(
            String operation,
            Partner partner,
            String correlationId
    ) {
        LOGGER.info(
                "partner_operation={} partner_id={} status={} correlation_id={}",
                operation,
                partner.id().value(),
                partner.status(),
                correlationId
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }

    private static String correlation(CorrelationId correlationId) {
        return Objects.requireNonNull(
                correlationId,
                "correlationId is required"
        ).value();
    }
}
