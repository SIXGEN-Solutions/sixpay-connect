package com.sixpay.payment.application.query;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchPaymentProjectionsQueryTest {

    @Test
    void defaultsSortToCreatedAtDescending() {
        SearchPaymentProjectionsQuery query =
                query(
                        50,
                        null,
                        null,
                        null,
                        null
                );

        assertThat(query.sort())
                .isEqualTo(
                        PaymentSearchSort.CREATED_AT_DESC
                );
    }

    @Test
    void rejectsInvalidPageSizes() {
        assertThatThrownBy(() ->
                query(
                        0,
                        null,
                        null,
                        null,
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "between 1 and 200"
                );

        assertThatThrownBy(() ->
                query(
                        201,
                        null,
                        null,
                        null,
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "between 1 and 200"
                );
    }

    @Test
    void rejectsReversedCreatedAtRange() {
        Instant from =
                Instant.parse("2026-08-09T18:01:00Z");
        Instant to =
                Instant.parse("2026-08-09T18:00:00Z");

        assertThatThrownBy(() ->
                query(
                        50,
                        from,
                        to,
                        null,
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "createdFrom"
                );
    }

    @Test
    void rejectsNegativeAmountsAndReversedAmountRange() {
        assertThatThrownBy(() ->
                query(
                        50,
                        null,
                        null,
                        new BigDecimal("-1.00"),
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "amountMin"
                );

        assertThatThrownBy(() ->
                query(
                        50,
                        null,
                        null,
                        null,
                        new BigDecimal("-1.00")
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "amountMax"
                );

        assertThatThrownBy(() ->
                query(
                        50,
                        null,
                        null,
                        new BigDecimal("200.00"),
                        new BigDecimal("100.00")
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "must not exceed"
                );
    }

    private static SearchPaymentProjectionsQuery query(
            int size,
            Instant createdFrom,
            Instant createdTo,
            BigDecimal amountMin,
            BigDecimal amountMax
    ) {
        return new SearchPaymentProjectionsQuery(
                null,
                size,
                null,
                null,
                null,
                null,
                null,
                null,
                createdFrom,
                createdTo,
                amountMin,
                amountMax,
                null,
                null
        );
    }
}
