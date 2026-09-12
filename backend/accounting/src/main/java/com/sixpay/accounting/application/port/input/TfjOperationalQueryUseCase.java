package com.sixpay.accounting.application.port.input;

import com.sixpay.accounting.domain.model.TfjOperationalCategory;
import com.sixpay.accounting.domain.model.TfjOperationalSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TfjOperationalQueryUseCase {

    Page search(
            LocalDate businessDate,
            TfjOperationalCategory category,
            String paymentReference,
            String bankPostingReference,
            int page,
            int size
    );

    TfjOperationalSnapshot findByConfirmationId(UUID confirmationId);

    record Page(
            List<TfjOperationalSnapshot> content,
            int page,
            int size,
            long totalElements
    ) {
        public int totalPages() {
            if (size <= 0 || totalElements == 0) {
                return 0;
            }
            return (int) ((totalElements + size - 1) / size);
        }
    }
}
