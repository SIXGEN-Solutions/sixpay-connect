package com.sixpay.partner.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.partner.application.command.CreatePartnerCommand;
import com.sixpay.partner.application.command.DecidePartnerCommand;
import com.sixpay.partner.application.command.PartnerDecision;
import com.sixpay.partner.application.port.out.PartnerAuditRecord;
import com.sixpay.partner.application.port.out.PartnerAuditTrail;
import com.sixpay.partner.application.port.out.PartnerAuditResult;
import com.sixpay.partner.application.port.out.PartnerThresholdHistory;
import com.sixpay.partner.application.port.out.PartnerIdempotencyStore;
import com.sixpay.partner.application.port.out.PartnerOperationMetrics;
import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerStatus;
import com.sixpay.partner.domain.repository.PartnerRepository;
import com.sixpay.partner.events.PartnerIntegrationEvent;
import com.sixpay.partner.events.PartnerStatusChangedIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerApplicationServiceTest {

    private static final PartnerId PARTNER_ID =
            new PartnerId(UUID.fromString("5fd3fcce-df38-4a0a-af60-38ff459bc272"));
    private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");

    private InMemoryPartnerRepository repository;
    private List<PartnerIntegrationEvent> events;
    private List<PartnerAuditRecord> audits;
    private PartnerApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPartnerRepository();
        events = new ArrayList<>();
        audits = new ArrayList<>();
        PartnerAuditTrail auditTrail = new PartnerAuditTrail() {
            @Override
            public void append(PartnerAuditRecord record) {
                audits.add(record);
            }

            @Override
            public PartnerAuditResult findByPartnerIdAndPeriod(
                    PartnerId partnerId,
                    Instant from,
                    Instant to,
                    int page,
                    int size
            ) {
                var filtered = audits.stream()
                        .filter(audit -> audit.partnerId().equals(partnerId))
                        .filter(audit -> !audit.occurredAt().isBefore(from) && !audit.occurredAt().isAfter(to))
                        .toList();
                var fromIndex = Math.min(page * size, filtered.size());
                var toIndex = Math.min(fromIndex + size, filtered.size());
                return new PartnerAuditResult(filtered.subList(fromIndex, toIndex), filtered.size());
            }
        };
        PartnerThresholdHistory thresholdHistory = ignored -> {
        };
        service = new PartnerApplicationService(
                repository,
                PARTNER_ID::value,
                events::add,
                auditTrail,
                thresholdHistory,
                new InMemoryIdempotencyStore(),
                new InMemoryMetrics(),
                () -> NOW
        );
    }

    private static final class InMemoryMetrics implements PartnerOperationMetrics {

        @Override
        public void succeeded(Operation operation) {
        }

        @Override
        public void replayed(Operation operation) {
        }

        @Override
        public void rejected(Rejection rejection) {
        }
    }

    @Test
    void createsThenApprovesPartnerAtomicallyThroughPorts() {
        var created = service.create(new CreatePartnerCommand(
                "Acme Payments",
                "Alice Ops",
                "alice.ops@example.com",
                Set.of("PAYMENT"),
                "admin@sixpay",
                CorrelationId.of("corr-001"),
                "idem-001"
        ));

        var approved = service.decide(new DecidePartnerCommand(
                PARTNER_ID,
                PartnerDecision.APPROVE,
                null,
                "manager@sixpay",
                CorrelationId.of("corr-002"),
                "idem-002"
        ));

        assertThat(created.status()).isEqualTo(PartnerStatus.PENDING_VALIDATION);
        assertThat(approved.status()).isEqualTo(PartnerStatus.ACTIVE);
        assertThat(events).hasSize(2);
        assertThat(events.get(1)).isInstanceOf(PartnerStatusChangedIntegrationEvent.class);
        assertThat(audits)
                .extracting(PartnerAuditRecord::action)
                .containsExactly("PARTNER_CREATED", "PARTNER_APPROVED");
    }

    @Test
    void replaysCreateWithoutRepeatingSideEffects() {
        var command = new CreatePartnerCommand(
                "Acme Payments",
                "Alice Ops",
                "alice.ops@example.com",
                Set.of("PAYMENT"),
                "admin@sixpay",
                CorrelationId.of("corr-replay-001"),
                "idem-replay-001"
        );

        var first = service.create(command);
        var replay = service.create(command);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(events).hasSize(1);
        assertThat(audits).hasSize(1);
    }

    private static final class InMemoryIdempotencyStore implements PartnerIdempotencyStore {

        private final Map<String, PartnerId> completed = new LinkedHashMap<>();

        @Override
        public void lock(String operation, String idempotencyKey) {
            // Unit tests execute serially; PostgreSQL provides the production transaction lock.
        }

        @Override
        public Optional<PartnerId> findCompleted(String operation, String idempotencyKey) {
            return Optional.ofNullable(completed.get(operation + ":" + idempotencyKey));
        }

        @Override
        public void complete(
                String operation,
                String idempotencyKey,
                PartnerId partnerId,
                Instant completedAt
        ) {
            completed.put(operation + ":" + idempotencyKey, partnerId);
        }
    }

    private static final class InMemoryPartnerRepository implements PartnerRepository {

        private final Map<PartnerId, Partner> partners = new LinkedHashMap<>();

        @Override
        public Partner save(Partner partner) {
            partners.put(partner.id(), partner);
            return partner;
        }

        @Override
        public Optional<Partner> findById(PartnerId partnerId) {
            return Optional.ofNullable(partners.get(partnerId));
        }

        @Override
        public boolean existsById(PartnerId partnerId) {
            return partners.containsKey(partnerId);
        }
    }

}
