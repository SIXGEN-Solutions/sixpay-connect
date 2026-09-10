package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.TfjConfirmationConflictException;
import com.sixpay.accounting.application.port.output.TfjConfirmationRepository;
import com.sixpay.accounting.application.port.output.TfjMatchRepository;
import com.sixpay.accounting.domain.model.TfjConfirmation;
import com.sixpay.accounting.domain.model.TfjMatchStatus;
import com.sixpay.accounting.domain.model.TfjReceiptStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TfjIngestionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TfjIngestionService.class);

    private final TfjConfirmationRepository repository;
    private final TfjMatchRepository matchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TfjIngestionService(
            TfjConfirmationRepository repository,
            TfjMatchRepository matchRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Transactional
    public TfjIngestionResult ingest(TfjConfirmation candidate) {
        Objects.requireNonNull(candidate, "candidate");

        TfjConfirmation normalized =
                candidate.withLogicalPayloadHash(
                        TfjLogicalFingerprint.sha256(candidate)
                );

        var byConfirmation = repository.findByConfirmationId(
                normalized.confirmationId()
        );
        if (byConfirmation.isPresent()) {
            return resolveReplay(byConfirmation.orElseThrow(), normalized);
        }

        var byIdempotency = repository.findByIdempotencyKey(
                normalized.idempotencyKey()
        );
        if (byIdempotency.isPresent()) {
            return resolveReplay(byIdempotency.orElseThrow(), normalized);
        }

        List<UUID> matches = matchRepository.findPaymentIds(
                normalized.financialInstitutionCode(),
                normalized.businessDate(),
                normalized.paymentReference(),
                normalized.bankPostingReference()
        );

        TfjConfirmation matched;
        if (matches.isEmpty()) {
            matched = normalized.withMatch(
                    TfjMatchStatus.UNMATCHED,
                    null
            );
        } else if (matches.size() == 1) {
            matched = normalized.withMatch(
                    TfjMatchStatus.MATCHED,
                    matches.getFirst()
            );
        } else {
            matched = normalized.withMatch(
                    TfjMatchStatus.AMBIGUOUS,
                    null
            );
        }

        TfjConfirmation persisted = repository.save(matched);

        if (persisted.matchStatus() != TfjMatchStatus.MATCHED) {
            LOGGER.warn(
                    "TFJ confirmation quarantined: confirmationId={}, matchStatus={}, "
                            + "financialInstitutionCode={}, businessDate={}, "
                            + "paymentReference={}, bankPostingReference={}, correlationId={}",
                    persisted.confirmationId(),
                    persisted.matchStatus(),
                    persisted.financialInstitutionCode(),
                    persisted.businessDate(),
                    persisted.paymentReference(),
                    persisted.bankPostingReference(),
                    persisted.correlationId()
            );
        }

        if (persisted.matchStatus() == TfjMatchStatus.MATCHED
                && persisted.terminal()) {
            eventPublisher.publishEvent(
                    new TfjFinalityReadyEvent(persisted)
            );
        }

        TfjReceiptStatus receiptStatus =
                persisted.matchStatus() == TfjMatchStatus.MATCHED
                        ? TfjReceiptStatus.ACCEPTED_FOR_MATCHING
                        : TfjReceiptStatus.QUARANTINED;

        return new TfjIngestionResult(
                persisted.confirmationId(),
                receiptStatus,
                persisted.receivedAt()
        );
    }

    private TfjIngestionResult resolveReplay(
            TfjConfirmation existing,
            TfjConfirmation candidate
    ) {
        if (!existing.logicalPayloadHash().equals(
                candidate.logicalPayloadHash()
        )) {
            LOGGER.error(
                    "TFJ confirmation conflict quarantined: confirmationId={}, "
                            + "financialInstitutionCode={}, businessDate={}, "
                            + "paymentReference={}, bankPostingReference={}, correlationId={}",
                    existing.confirmationId(),
                    existing.financialInstitutionCode(),
                    existing.businessDate(),
                    existing.paymentReference(),
                    existing.bankPostingReference(),
                    existing.correlationId()
            );
            throw new TfjConfirmationConflictException(
                    "TFJ confirmation replay conflicts with durably stored payload"
            );
        }

        return new TfjIngestionResult(
                existing.confirmationId(),
                TfjReceiptStatus.IDENTICAL_REPLAY,
                existing.receivedAt()
        );
    }
}
