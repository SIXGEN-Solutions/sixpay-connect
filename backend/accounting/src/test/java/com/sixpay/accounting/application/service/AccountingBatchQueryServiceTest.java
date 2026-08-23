package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingBatchNotFoundException;
import com.sixpay.accounting.application.port.output.AccountingBatchQueryPort;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountingBatchQueryServiceTest {

    private final AccountingBatchQueryPort queryPort =
            mock(AccountingBatchQueryPort.class);

    private final AccountingBatchQueryService service =
            new AccountingBatchQueryService(queryPort);

    @Test
    void throwsWhenBatchDoesNotExist() {
        var id = new AccountingBatchId(UUID.randomUUID());
        when(queryPort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AccountingBatchNotFoundException.class);
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThatThrownBy(() -> service.search(null, null, 0, 201))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
