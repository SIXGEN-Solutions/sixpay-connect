package com.sixpay.customer.observation.application.port.output.query;

import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerCursor;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPosition;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPosition;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;

/**
 * Authenticates, decodes and creates opaque query cursors.
 */
public interface ObservedCustomerCursorCodec {

    ObservedCustomerSearchCriteria decodeSearch(
            SearchObservedCustomersQuery query
    );

    ObservedCustomerPaymentCriteria decodePayments(
            ListObservedCustomerPaymentsQuery query
    );

    ObservedCustomerCursor encodeSearch(
            ObservedCustomerSearchCriteria criteria,
            ObservedCustomerSearchPosition position
    );

    ObservedCustomerCursor encodePayments(
            ObservedCustomerPaymentCriteria criteria,
            ObservedCustomerPaymentPosition position
    );
}
