package com.sixpay.customer.observation.infrastructure.query.mapper;

import com.sixpay.customer.observation.infrastructure.persistence.protection
        .ObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedCustomerDetailRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedCustomerSummaryRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedInstitutionRow;
import com.sixpay.customer.observation.infrastructure.query.model
        .ObservedPaymentRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ObservedCustomerQueryRowMapperTest {

    private static final Instant FIRST =
            Instant.parse("2026-08-04T18:00:00Z");

    private static final Instant LAST =
            Instant.parse("2026-08-04T19:00:00Z");

    private final ObservedCustomerQueryRowMapper mapper =
            new ObservedCustomerQueryRowMapper(
                    protector()
            );

    @Test
    void summaryRevealsOnlySelectedRowAndMasksNiu() {
        var view = mapper.toSummary(
                summaryRow()
        );

        assertEquals(
                "****3456",
                view.niu().maskedValue()
        );
        assertEquals(
                "Société ABC SARL",
                view.legalName()
        );
        assertFalse(
                view.toString().contains("M0123456")
        );
        assertFalse(
                view.toString().contains(
                        "Société ABC SARL"
                )
        );
    }

    @Test
    void detailUsesTechnicalAccountIdAndNeverFingerprint() {
        UUID accountId = UUID.fromString(
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        );

        ObservedCustomerDetailRow row =
                new ObservedCustomerDetailRow(
                        summaryRow().observedCustomerId(),
                        "protected:M0123456",
                        "protected:Société ABC SARL",
                        "***-***-1234",
                        "a***@example.com",
                        List.of(
                                new ObservedInstitutionRow(
                                        UUID.randomUUID(),
                                        "SIXPAY_BANK",
                                        FIRST,
                                        LAST,
                                        List.of(
                                                new ObservedInstitutionRow
                                                        .AccountRow(
                                                        accountId,
                                                        "•••• 1234"
                                                )
                                        )
                                )
                        ),
                        FIRST,
                        LAST,
                        2,
                        1,
                        0,
                        "DEBITED",
                        null,
                        LAST,
                        2,
                        "event:2"
                );

        var view = mapper.toDetail(row);

        assertEquals(
                accountId.toString(),
                view.institutions()
                        .getFirst()
                        .accounts()
                        .getFirst()
                        .reference()
        );
        assertEquals(
                "•••• 1234",
                view.institutions()
                        .getFirst()
                        .accounts()
                        .getFirst()
                        .maskedValue()
        );
    }

    @Test
    void paymentRowMapsCanonicalCustomerOwnedStatus() {
        var view = mapper.toPayment(
                new ObservedPaymentRow(
                        UUID.randomUUID(),
                        "PAY-2026-000123",
                        "SIXPAY_BANK",
                        new BigDecimal("15000.00"),
                        "XAF",
                        "DEBITED",
                        null,
                        FIRST,
                        LAST
                )
        );

        assertEquals("DEBITED", view.status().name());
        assertEquals("XAF", view.currency());
    }

    private static ObservedCustomerSummaryRow summaryRow() {
        return new ObservedCustomerSummaryRow(
                UUID.fromString(
                        "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                ),
                "protected:M0123456",
                "protected:Société ABC SARL",
                "***-***-1234",
                "a***@example.com",
                FIRST,
                LAST,
                2,
                1,
                0,
                "DEBITED",
                null,
                LAST,
                2
        );
    }

    private static ObservedCustomerDataProtector protector() {
        return new ObservedCustomerDataProtector() {
            @Override
            public String protect(String plaintext) {
                return "protected:" + plaintext;
            }

            @Override
            public String reveal(String protectedValue) {
                return protectedValue.substring(
                        "protected:".length()
                );
            }

            @Override
            public String searchHash(String normalizedValue) {
                return "hash:" + normalizedValue;
            }
        };
    }
}
