package com.sixpay.partner.infrastructure.audit;

import com.sixpay.partner.application.port.out.PartnerThresholdHistory;
import com.sixpay.partner.application.port.out.PartnerThresholdHistoryRecord;
import org.springframework.stereotype.Repository;

@Repository
public class PartnerThresholdHistoryAdapter implements PartnerThresholdHistory {

    private final PartnerThresholdHistorySpringDataRepository repository;

    public PartnerThresholdHistoryAdapter(PartnerThresholdHistorySpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(PartnerThresholdHistoryRecord record) {
        repository.save(new PartnerThresholdHistoryJpaEntity(record));
    }
}
