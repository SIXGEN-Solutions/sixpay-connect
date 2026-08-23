package com.sixpay.accounting.api;

import com.sixpay.accounting.application.exception.AccountingBatchNotFoundException;
import com.sixpay.accounting.application.port.input.AccountingBatchQueryUseCase;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AccountingBatchQueryController.class
)
@ContextConfiguration(
        classes = {
                AccountingBatchQueryController.class,
                AccountingBatchQueryExceptionHandler.class,
                AccountingBatchQueryControllerTest
                        .MethodSecurityConfiguration.class
        }
)
class AccountingBatchQueryControllerTest {

    private static final UUID BATCH_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "22222222-2222-4222-8222-222222222222"
            );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountingBatchQueryUseCase query;

    @Test
    @WithMockUser(
            username = "admin@sixpay",
            roles = "ADMIN"
    )
    void searchesAccountingBatchesForAuthorizedAdministrator()
            throws Exception {

        AccountingBatch batch =
                batch();

        when(
                query.search(
                        null,
                        null,
                        0,
                        20
                )
        ).thenReturn(
                new AccountingBatchQueryUseCase
                        .AccountingBatchPage(
                        List.of(batch),
                        0,
                        20,
                        1
                )
        );

        mockMvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "accounting-batches"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].batchId"
                        ).value(
                                BATCH_ID.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].status"
                        ).value(
                                "NOT_COMPLETED"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].itemCount"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.totalElements"
                        ).value(1)
                );
    }

    @Test
    @WithMockUser(
            username = "auditor@sixpay",
            roles = "AUDITOR"
    )
    void readsAccountingBatchDetailForAuditor()
            throws Exception {

        AccountingBatch batch =
                batch();

        when(
                query.findById(
                        any()
                )
        ).thenReturn(
                batch
        );

        mockMvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "accounting-batches/{batchId}",
                                BATCH_ID
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.batchId"
                        ).value(
                                BATCH_ID.toString()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(
                                "NOT_COMPLETED"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.itemCount"
                        ).value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.items[0].paymentId"
                        ).value(
                                PAYMENT_ID.toString()
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "partner@sixpay",
            roles = "PARTNER"
    )
    void forbidsAccountingQueryForPartnerRole()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "accounting-batches"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void rejectsUnauthenticatedAccountingQuery()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "accounting-batches"
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    @WithMockUser(
            username = "manager@sixpay",
            roles = "MANAGER"
    )
    void returns404ForUnknownAccountingBatch()
            throws Exception {

        AccountingBatchId batchId =
                new AccountingBatchId(
                        BATCH_ID
                );

        when(
                query.findById(
                        any()
                )
        ).thenThrow(
                new AccountingBatchNotFoundException(
                        batchId
                )
        );

        mockMvc.perform(
                        get(
                                "/internal/api/v1/"
                                        + "accounting-batches/{batchId}",
                                BATCH_ID
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath(
                                "$.title"
                        ).value(
                                "Accounting batch not found"
                        )
                );
    }

    private static AccountingBatch batch() {
        return new AccountingBatch(
                new AccountingBatchId(
                        BATCH_ID
                ),
                new AccountingBatchIdempotencyKey(
                        "aaaaaaaaaaaaaaaa"
                                + "aaaaaaaaaaaaaaaa"
                                + "aaaaaaaaaaaaaaaa"
                                + "aaaaaaaaaaaaaaaa"
                ),
                LocalDate.of(
                        2026,
                        8,
                        23
                ),
                "LAREGIONALE",
                Instant.parse(
                        "2026-08-23T03:00:00Z"
                ),
                AccountingBatchStatus.NOT_COMPLETED,
                List.of(
                        batchItem()
                )
        );
    }

    private static AccountingBatchItem batchItem() {
        return new AccountingBatchItem(
                PAYMENT_ID,
                "PAY-ACC-001",
                "PARTNER-001",
                new BigDecimal(
                        "1500.00"
                ),
                Currency.getInstance(
                        "XAF"
                ),
                Instant.parse(
                        "2026-08-23T02:30:00Z"
                ),
                LocalDate.of(
                        2026,
                        8,
                        23
                ),
                "BANK-POST-001",
                "SUCCESS",
                Instant.parse(
                        "2026-08-23T02:35:00Z"
                ),
                AccountingBatchItemStatus.PENDING
        );
    }

    @Configuration(
            proxyBeanMethods = false
    )
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}