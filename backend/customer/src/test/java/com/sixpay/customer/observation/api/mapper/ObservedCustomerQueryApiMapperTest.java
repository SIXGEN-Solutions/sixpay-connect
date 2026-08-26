package com.sixpay.customer.observation.api.mapper;

import com.sixpay.customer.observation.application.query
        .MaskedIdentifierView;
import com.sixpay.customer.observation.application.query
        .ObservedAccountView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPage;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSummaryView;
import com.sixpay.customer.observation.application.query
        .ObservedInstitutionView;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ObservedCustomerQueryApiMapperTest {

    private static final Instant FIRST =
            Instant.parse("2026-08-04T18:00:00Z");

    private static final Instant LAST =
            Instant.parse("2026-08-04T19:00:00Z");

    private final ObservedCustomerQueryApiMapper mapper =
            new ObservedCustomerQueryApiMapper();

    @Test
    void mapsSearchPageWithoutChangingMaskedValues() {
        ObservedCustomerSummaryView summary =
                new ObservedCustomerSummaryView(
                        customerId(),
                        new MaskedIdentifierView("****3456"),
                        "Société ABC SARL",
                        new MaskedIdentifierView("***-***-1234"),
                        new MaskedIdentifierView("a***@example.com"),
                        FIRST,
                        LAST,
                        2,
                        1,
                        0,
                        ObservedPaymentStatus.DEBITED,
                        null,
                        LAST,
                        2
                );

        var response = mapper.toResponse(
                new ObservedCustomerSearchPage(
                        List.of(summary),
                        1,
                        true,
                        new ObservedCustomerCursor(
                                "signed-cursor"
                        ),
                        LAST
                )
        );

        assertEquals("****3456",
                response.items().getFirst().niu().maskedValue());
        assertEquals("signed-cursor", response.nextCursor());
        assertEquals(LAST, response.snapshotAt());
    }

    @Test
    void detailMapsOnlyMaskedAccountReference() {
        ObservedCustomerDetailView detail =
                new ObservedCustomerDetailView(
                        customerId(),
                        new MaskedIdentifierView("****3456"),
                        "Société ABC SARL",
                        null,
                        null,
                        List.of(
                                new ObservedInstitutionView(
                                        "SIXPAY_BANK",
                                        FIRST,
                                        LAST,
                                        List.of(
                                                new ObservedAccountView(
                                                        "account-row-id",
                                                        "•••• 1234"
                                                )
                                        )
                                )
                        ),
                        FIRST,
                        LAST,
                        1,
                        0,
                        0,
                        ObservedPaymentStatus.RECEIVED,
                        null,
                        LAST,
                        1,
                        "event:1"
                );

        var response = mapper.toResponse(detail);

        assertEquals(
                "account-row-id",
                response.institutions()
                        .getFirst()
                        .accounts()
                        .getFirst()
                        .reference()
        );
        assertEquals(
                "•••• 1234",
                response.institutions()
                        .getFirst()
                        .accounts()
                        .getFirst()
                        .maskedValue()
        );
        assertFalse(
                response.toString().contains(
                        "accountBindingFingerprint"
                )
        );
    }

    private static ObservedCustomerId customerId() {
        return ObservedCustomerId.of(
                UUID.fromString(
                        "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                )
        );
    }
}
