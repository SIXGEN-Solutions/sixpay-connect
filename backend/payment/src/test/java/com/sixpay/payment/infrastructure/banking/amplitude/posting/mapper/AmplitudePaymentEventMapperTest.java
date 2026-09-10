package com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventRequest;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmplitudePaymentEventMapperTest {

    private static final Instant T0 =
            Instant.parse("2026-09-06T18:00:00Z");

    private final AmplitudePaymentEventMapper mapper =
            new AmplitudePaymentEventMapper();

    @Test
    void mapsFinalizedSnapshotToReducedAmplitudePaymentEvent() {
        AmplitudePaymentEventRequest request =
                mapper.toRequest(
                        finalizedSnapshot(),
                        context()
                );

        assertEquals(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                request.paymentReference()
        );
        assertEquals("v1", request.snapshotVersion());
        assertEquals(2, request.providerEntries().size());

        assertEquals(
                "VIRPAY",
                request.providerEvent().nature()
        );
        assertEquals(
                "XAF",
                request.providerEvent().currency()
        );
        assertEquals(
                "00001-12345678901-42",
                request.providerEvent().debtorAccountReference()
        );
        assertEquals(
                "00001-99999999999-17",
                request.providerEvent().creditorAccountReference()
        );
        assertEquals(
                "2026-09-06/PAIEMENT/"
                        + "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV/"
                        + "12345678901",
                request.providerEvent().label()
        );
        assertTrue(request.providerEvent().nightMode());

        assertEquals(
                "D",
                request.providerEntries().get(0).direction()
        );
        assertEquals(
                "C",
                request.providerEntries().get(1).direction()
        );
        assertEquals(
                0,
                new BigDecimal("1000").compareTo(
                        request.providerEntries().get(0).amount()
                )
        );
        assertEquals(
                request.providerEntries().get(0).amount(),
                request.providerEntries().get(1).amount()
        );
    }

    @Test
    void rejectsDraftSnapshot() {
        PaymentFinancialEventSnapshot snapshot =
                draftSnapshot();

        snapshot.addEntry(
                entry(
                        "11111111-1111-4111-8111-111111111111",
                        1,
                        FinancialEntryDirection.DEBIT,
                        "00001-12345678901-42"
                )
        );
        snapshot.addEntry(
                entry(
                        "22222222-2222-4222-8222-222222222222",
                        2,
                        FinancialEntryDirection.CREDIT,
                        "00001-99999999999-17"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRequest(snapshot, context())
        );
    }

    @Test
    void rejectsNonApprovedNature() {
        AmplitudePaymentEventMappingContext invalid =
                new AmplitudePaymentEventMappingContext(
                        "PAY",
                        101L,
                        LocalDate.of(2026, 9, 6),
                        false,
                        "OTHER",
                        "SIXPAY",
                        T0.plusSeconds(2)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRequest(
                        finalizedSnapshot(),
                        invalid
                )
        );
    }

    @Test
    void rejectsAccountOutsideAgeNcpClcFormat() {
        PaymentFinancialEventSnapshot snapshot =
                PaymentFinancialEventSnapshot.draft(
                        UUID.fromString(
                                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                        ),
                        new PaymentId(
                                UUID.fromString(
                                        "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
                                )
                        ),
                        PublicPaymentReference.of(
                                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                        ),
                        FinancialInstitutionCode.of("LRB"),
                        "debtor-ref",
                        "00001-99999999999-17",
                        money(),
                        "v1",
                        T0
                );

        snapshot.addEntry(
                entry(
                        "11111111-1111-4111-8111-111111111111",
                        1,
                        FinancialEntryDirection.DEBIT,
                        "debtor-ref"
                )
        );
        snapshot.addEntry(
                entry(
                        "22222222-2222-4222-8222-222222222222",
                        2,
                        FinancialEntryDirection.CREDIT,
                        "00001-99999999999-17"
                )
        );
        snapshot.finalizeAt(T0.plusSeconds(1));

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRequest(snapshot, context())
        );
    }

    @Test
    void requestContainsNoCommissionOrTaxFields() {
        AmplitudePaymentEventRequest request =
                mapper.toRequest(
                        finalizedSnapshot(),
                        context()
                );

        assertFalse(
                request.providerEvent()
                        .getClass()
                        .getRecordComponents()
                        .length == 0
        );

        for (var component :
                request.providerEvent()
                        .getClass()
                        .getRecordComponents()) {
            assertFalse(
                    component.getName()
                            .toLowerCase()
                            .contains("commission")
            );
            assertFalse(
                    component.getName()
                            .toLowerCase()
                            .contains("tax")
            );
        }
    }

    private static PaymentFinancialEventSnapshot finalizedSnapshot() {
        PaymentFinancialEventSnapshot snapshot =
                draftSnapshot();

        snapshot.addEntry(
                entry(
                        "11111111-1111-4111-8111-111111111111",
                        1,
                        FinancialEntryDirection.DEBIT,
                        "00001-12345678901-42"
                )
        );
        snapshot.addEntry(
                entry(
                        "22222222-2222-4222-8222-222222222222",
                        2,
                        FinancialEntryDirection.CREDIT,
                        "00001-99999999999-17"
                )
        );
        snapshot.finalizeAt(T0.plusSeconds(1));
        return snapshot;
    }

    private static PaymentFinancialEventSnapshot draftSnapshot() {
        return PaymentFinancialEventSnapshot.draft(
                UUID.fromString(
                        "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                ),
                new PaymentId(
                        UUID.fromString(
                                "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
                        )
                ),
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                ),
                FinancialInstitutionCode.of("LRB"),
                "00001-12345678901-42",
                "00001-99999999999-17",
                money(),
                "v1",
                T0
        );
    }

    private static PaymentFinancialEntrySnapshot entry(
            String id,
            int sequence,
            FinancialEntryDirection direction,
            String accountReference
    ) {
        return new PaymentFinancialEntrySnapshot(
                UUID.fromString(id),
                sequence,
                direction,
                accountReference,
                money(),
                T0
        );
    }

    private static Money money() {
        return Money.of(
                new BigDecimal("1000.00"),
                "XAF"
        );
    }

    private static AmplitudePaymentEventMappingContext context() {
        return new AmplitudePaymentEventMappingContext(
                "PAY",
                101L,
                LocalDate.of(2026, 9, 6),
                true,
                "VIRPAY",
                "SIXPAY",
                T0.plusSeconds(2)
        );
    }
}
