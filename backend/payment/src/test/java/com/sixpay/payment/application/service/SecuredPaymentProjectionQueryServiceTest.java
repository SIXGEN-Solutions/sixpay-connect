package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.security.PaymentObjectAccessPort;
import com.sixpay.payment.application.port.output.security.PaymentProjectionReadPort;
import com.sixpay.payment.application.query.PaymentSearchSort;
import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.application.security.PaymentAccessDeniedException;
import com.sixpay.payment.application.security.PaymentAccessPolicy;
import com.sixpay.payment.application.security.PaymentAction;
import com.sixpay.payment.application.security.PaymentObjectAccessDescriptor;
import com.sixpay.payment.application.security.PaymentVisibilityScope;
import com.sixpay.payment.application.view.PaymentProjectionViews;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SecuredPaymentProjectionQueryServiceTest {

    private PaymentProjectionReadPort projectionReadPort;
    private PaymentObjectAccessPort objectAccessPort;
    private PaymentAccessPolicy accessPolicy;
    private SecuredPaymentProjectionQueryService service;

    @BeforeEach
    void setUp() {
        projectionReadPort =
                Mockito.mock(PaymentProjectionReadPort.class);
        objectAccessPort =
                Mockito.mock(PaymentObjectAccessPort.class);
        accessPolicy =
                Mockito.mock(PaymentAccessPolicy.class);

        service = new SecuredPaymentProjectionQueryService(
                projectionReadPort,
                objectAccessPort,
                accessPolicy
        );
    }

    @Test
    void searchUsesVisibilityResolvedByAccessPolicy() {
        SearchPaymentProjectionsQuery query = query();
        PaymentVisibilityScope visibility =
                new PaymentVisibilityScope.Internal();
        PaymentProjectionViews.SearchPage expected =
                Mockito.mock(
                        PaymentProjectionViews.SearchPage.class
                );

        when(accessPolicy.requireSearchVisibility())
                .thenReturn(visibility);
        when(projectionReadPort.search(query, visibility))
                .thenReturn(expected);

        PaymentProjectionViews.SearchPage actual =
                service.search(query);

        assertThat(actual).isSameAs(expected);

        verify(accessPolicy).requireSearchVisibility();
        verify(projectionReadPort).search(query, visibility);
        verifyNoInteractions(objectAccessPort);
    }

    @Test
    void detailChecksObjectAccessBeforeReadingProjection() {
        UUID paymentUuid = UUID.randomUUID();
        PaymentId paymentId = new PaymentId(paymentUuid);

        PaymentObjectAccessDescriptor descriptor =
                new PaymentObjectAccessDescriptor(
                        paymentId,
                        PaymentSource.TRESOR_PAY,
                        "partner-001"
                );

        PaymentProjectionViews.Detail expected =
                Mockito.mock(
                        PaymentProjectionViews.Detail.class
                );

        when(objectAccessPort.findAccessDescriptor(paymentId))
                .thenReturn(Optional.of(descriptor));
        when(projectionReadPort.findById(paymentId))
                .thenReturn(Optional.of(expected));

        Optional<PaymentProjectionViews.Detail> actual =
                service.findById(paymentUuid);

        assertThat(actual).containsSame(expected);

        verify(objectAccessPort)
                .findAccessDescriptor(paymentId);
        verify(accessPolicy)
                .requireObjectAccess(
                        PaymentAction.READ,
                        descriptor
                );
        verify(projectionReadPort)
                .findById(paymentId);
    }

    @Test
    void missingAccessDescriptorFailsClosedWithoutReadingProjection() {
        UUID paymentUuid = UUID.randomUUID();
        PaymentId paymentId = new PaymentId(paymentUuid);

        when(objectAccessPort.findAccessDescriptor(paymentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.findById(paymentUuid)
        )
                .isInstanceOf(
                        PaymentAccessDeniedException.class
                )
                .hasMessageContaining(
                        "not visible"
                );

        verify(objectAccessPort)
                .findAccessDescriptor(paymentId);
        verifyNoInteractions(accessPolicy);
        verify(
                projectionReadPort,
                never()
        ).findById(paymentId);
    }

    @Test
    void policyRejectionStopsProjectionRead() {
        UUID paymentUuid = UUID.randomUUID();
        PaymentId paymentId = new PaymentId(paymentUuid);

        PaymentObjectAccessDescriptor descriptor =
                new PaymentObjectAccessDescriptor(
                        paymentId,
                        PaymentSource.TRESOR_PAY,
                        "partner-002"
                );

        when(objectAccessPort.findAccessDescriptor(paymentId))
                .thenReturn(Optional.of(descriptor));

        org.mockito.Mockito.doThrow(
                new PaymentAccessDeniedException(
                        "Forbidden Payment"
                )
        ).when(accessPolicy)
                .requireObjectAccess(
                        PaymentAction.READ,
                        descriptor
                );

        assertThatThrownBy(() ->
                service.findById(paymentUuid)
        )
                .isInstanceOf(
                        PaymentAccessDeniedException.class
                );

        verify(
                projectionReadPort,
                never()
        ).findById(paymentId);
    }

    @Test
    void authorizedMissingProjectionReturnsEmpty() {
        UUID paymentUuid = UUID.randomUUID();
        PaymentId paymentId = new PaymentId(paymentUuid);

        PaymentObjectAccessDescriptor descriptor =
                new PaymentObjectAccessDescriptor(
                        paymentId,
                        PaymentSource.TRESOR_PAY,
                        "partner-001"
                );

        when(objectAccessPort.findAccessDescriptor(paymentId))
                .thenReturn(Optional.of(descriptor));
        when(projectionReadPort.findById(paymentId))
                .thenReturn(Optional.empty());

        assertThat(service.findById(paymentUuid))
                .isEmpty();

        verify(accessPolicy)
                .requireObjectAccess(
                        PaymentAction.READ,
                        descriptor
                );
    }

    @Test
    void rejectsNullPaymentIdentifierBeforeAccessLookup() {
        assertThatThrownBy(() ->
                service.findById(null)
        ).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(
                objectAccessPort,
                projectionReadPort,
                accessPolicy
        );
    }

    private static SearchPaymentProjectionsQuery query() {
        return new SearchPaymentProjectionsQuery(
                null,
                50,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PaymentSearchSort.CREATED_AT_DESC
        );
    }
}
