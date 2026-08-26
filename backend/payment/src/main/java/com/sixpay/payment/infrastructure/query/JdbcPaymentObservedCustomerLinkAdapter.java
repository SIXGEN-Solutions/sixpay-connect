package com.sixpay.payment.infrastructure.query;

import com.sixpay.payment.application.exception.PaymentQueryUnavailableException;
import com.sixpay.payment.application.port.output.query.PaymentObservedCustomerLinkPort;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public final class JdbcPaymentObservedCustomerLinkAdapter
        implements PaymentObservedCustomerLinkPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcPaymentObservedCustomerLinkAdapter(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "jdbcTemplate is required"
        );
    }

    @Override
    public void link(UUID paymentId, UUID observedCustomerId) {
        Objects.requireNonNull(paymentId, "paymentId is required");
        Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );

        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO payment_observed_customer_link (
                        payment_id,
                        observed_customer_id
                    ) VALUES (
                        :paymentId,
                        :observedCustomerId
                    )
                    ON CONFLICT (payment_id)
                    DO UPDATE SET
                        observed_customer_id = EXCLUDED.observed_customer_id,
                        linked_at = CURRENT_TIMESTAMP
                    """,
                    new MapSqlParameterSource()
                            .addValue("paymentId", paymentId)
                            .addValue("observedCustomerId", observedCustomerId)
            );
        } catch (DataAccessException exception) {
            throw new PaymentQueryUnavailableException(
                    "Payment observed-customer link store is unavailable",
                    exception
            );
        }
    }
}
