package com.sixpay.partner.infrastructure.audit;

import com.sixpay.partner.application.port.output.PartnerAuditRecord;
import com.sixpay.partner.application.port.output.PartnerAuditResult;
import com.sixpay.partner.application.port.output.PartnerAuditTrail;
import com.sixpay.partner.domain.model.PartnerId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
@Repository
public class PartnerAuditTrailAdapter implements PartnerAuditTrail {

    private final PartnerAuditSpringDataRepository repository;

    public PartnerAuditTrailAdapter(PartnerAuditSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(PartnerAuditRecord record) {
        repository.save(new PartnerAuditJpaEntity(record));
    }

    @Override
    public PartnerAuditResult findByPartnerIdAndPeriod(
            PartnerId partnerId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        var result = repository.findByPartnerIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                        partnerId.value(),
                        from,
                        to,
                        PageRequest.of(page, size)
                );
        var records = result.stream()
                .map(PartnerAuditJpaEntity::toRecord)
                .toList();
        return new PartnerAuditResult(records, result.getTotalElements());
    }
}
