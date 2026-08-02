package com.sixpay.payment.infrastructure.query;

import com.sixpay.payment.application.port.out.security.PaymentObjectAccessPort;
import com.sixpay.payment.application.security.PaymentObjectAccessDescriptor;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Reads only the metadata required by object-level authorization.
 *
 * <p>The current Payment schema does not persist a Partner owner. The adapter
 * therefore returns a null Partner subject, which keeps Partner access
 * fail-closed while allowing authorized internal users.</p>
 */
@Component
public final class PaymentObjectAccessAdapter
        implements PaymentObjectAccessPort {

    private static final String FIND_ACCESS_DESCRIPTOR = """
            SELECT payment_source
              FROM payments
             WHERE payment_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PaymentObjectAccessAdapter(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "JDBC template"
        );
    }

    @Override
    public Optional<PaymentObjectAccessDescriptor>
            findAccessDescriptor(PaymentId paymentId) {

        Objects.requireNonNull(paymentId, "Payment ID");

        return jdbcTemplate.query(
                FIND_ACCESS_DESCRIPTOR,
                (resultSet, rowNumber) ->
                        new PaymentObjectAccessDescriptor(
                                paymentId,
                                PaymentSource.valueOf(
                                        resultSet.getString(
                                                "payment_source"
                                        )
                                ),
                                null
                        ),
                paymentId.value()
        ).stream().findFirst();
    }
}
