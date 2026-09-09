package com.sixpay.accounting.application.service;

import com.sixpay.accounting.configuration.AccountingModuleConfiguration;
import com.sixpay.accounting.application.port.output.TfjConfirmationRepository;
import com.sixpay.accounting.domain.model.TfjConfirmation;
import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class TfjFinalityPublicationService {

    public static final String EVENT_TYPE =
            "AccountingTfjFinalityResolved";

    private final IntegrationEventTransport transport;
    private final TfjConfirmationRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TfjFinalityPublicationService(
            IntegrationEventTransport transport,
            TfjConfirmationRepository repository,
            ObjectMapper objectMapper,
            @Qualifier(AccountingModuleConfiguration.ACCOUNTING_CLOCK)
            Clock clock
    ) {
        this.transport = Objects.requireNonNull(transport);
        this.repository = Objects.requireNonNull(repository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void afterCommit(TfjFinalityReadyEvent event) {
        publishOne(event.confirmation());
    }

    @Transactional
    public int publishPending(int limit) {
        var pending = repository.findPendingFinalityPublication(limit);
        pending.forEach(this::publishOne);
        return pending.size();
    }

    private void publishOne(TfjConfirmation confirmation) {
        if (confirmation.finalityPublishedAt() != null
                || !confirmation.terminal()
                || confirmation.matchedPaymentId() == null) {
            return;
        }

        transport.publish(
                new IntegrationEventEnvelope(
                        UUID.randomUUID(),
                        EVENT_TYPE,
                        1,
                        "PAYMENT",
                        confirmation.matchedPaymentId(),
                        confirmation.correlationId(),
                        confirmation.receivedAt(),
                        payload(confirmation)
                )
        );

        repository.save(
                confirmation.markFinalityPublished(clock.instant())
        );
    }

    private String payload(TfjConfirmation confirmation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("confirmationId", confirmation.confirmationId());
        payload.put(
                "financialInstitutionCode",
                confirmation.financialInstitutionCode()
        );
        payload.put("businessDate", confirmation.businessDate());
        payload.put("paymentReference", confirmation.paymentReference());
        payload.put(
                "bankPostingReference",
                confirmation.bankPostingReference()
        );
        payload.put("tfjBatchReference", confirmation.tfjBatchReference());
        payload.put("tfjStatus", confirmation.status().name());
        payload.put("confirmedAt", confirmation.confirmedAt());
        payload.put("matchedAt", confirmation.receivedAt());
        payload.put(
                "observationChannel",
                confirmation.observationChannel().name()
        );
        payload.put(
                "evidenceFingerprint",
                "v1:sha256:" + confirmation.logicalPayloadHash()
        );
        payload.put("failureCode", confirmation.failureCode());
        payload.put(
                "failureDescription",
                confirmation.failureDescription()
        );
        payload.put(
                "recoveryAction",
                confirmation.recoveryAction() == null
                        ? null
                        : confirmation.recoveryAction().name()
        );
        return objectMapper.writeValueAsString(payload);
    }
}
