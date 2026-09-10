package com.sixpay.accounting.application.port.output;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TfjMatchRepository {
    List<UUID> findPaymentIds(
            String financialInstitutionCode,
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference
    );
}
