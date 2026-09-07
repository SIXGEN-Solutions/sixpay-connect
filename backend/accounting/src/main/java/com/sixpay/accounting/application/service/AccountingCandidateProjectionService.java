package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.events.PaymentT0FinalizedForAccounting;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class AccountingCandidateProjectionService {
    private final AccountingCandidateProjectionRepository repository;
    private final Clock clock;

    public AccountingCandidateProjectionService(AccountingCandidateProjectionRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public AccountingCandidateProjection project(PaymentT0FinalizedForAccounting event) {
        Objects.requireNonNull(event, "event");
        var byEvent = repository.findByEventId(event.eventId());
        if (byEvent.isPresent()) return byEvent.get();
        var byBusiness = repository.findByBusinessIdentity(event.paymentId(), event.financialSnapshotId());
        if (byBusiness.isPresent()) return byBusiness.get();
        return repository.save(AccountingCandidateProjection.from(event, Instant.now(clock)));
    }
}
