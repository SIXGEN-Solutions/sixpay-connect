package com.sixpay.customer.observation.application.port.output.query;

import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;

/**
 * Validates and decodes opaque query cursors.
 *
 * <p>The returned query is canonical: its snapshot, sort, filters and page
 * size have been checked against the authenticated cursor payload.</p>
 */
public interface ObservedCustomerCursorCodec {

    SearchObservedCustomersQuery resolveSearch(
            SearchObservedCustomersQuery query
    );

    ListObservedCustomerPaymentsQuery resolvePayments(
            ListObservedCustomerPaymentsQuery query
    );
}
