package com.sixpay.accounting.domain.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface AccountingCutoffPolicy {
    AccountingSelectionWindow resolve(
            Instant runAt,
            AccountingCutoffMode mode,
            Optional<LocalDate> manualBusinessDate
    );
}
