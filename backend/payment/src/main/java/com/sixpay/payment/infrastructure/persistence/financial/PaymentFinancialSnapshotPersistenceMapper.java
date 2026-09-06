package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class PaymentFinancialSnapshotPersistenceMapper {

    PaymentFinancialEventSnapshotJpaEntity toNewEntity(
            PaymentFinancialEventSnapshot snapshot
    ) {
        return PaymentFinancialEventSnapshotJpaEntity
                .create(snapshot);
    }

    PaymentFinancialEventSnapshot toDomain(
            PaymentFinancialEventSnapshotJpaEntity entity
    ) {
        var entries = entity.entries().stream()
                .sorted(
                        Comparator.comparingInt(
                                PaymentFinancialEntrySnapshotJpaEntity
                                        ::sequence
                        )
                )
                .map(entry ->
                        new PaymentFinancialEntrySnapshot(
                                entry.entrySnapshotId(),
                                entry.sequence(),
                                entry.direction(),
                                entry.accountReference(),
                                Money.of(
                                        entry.amount(),
                                        entry.currency()
                                ),
                                entry.createdAt()
                        )
                )
                .toList();

        return PaymentFinancialEventSnapshot.reconstitute(
                entity.snapshotId(),
                new PaymentId(entity.paymentId()),
                PublicPaymentReference.of(
                        entity.publicPaymentReference()
                ),
                FinancialInstitutionCode.of(
                        entity.financialInstitutionCode()
                ),
                entity.debtorAccountReference(),
                entity.creditorAccountReference(),
                Money.of(
                        entity.requestedAmount(),
                        entity.requestedCurrency()
                ),
                entity.snapshotVersion(),
                entity.snapshotStatus(),
                entity.createdAt(),
                entity.finalizedAt(),
                entries
        );
    }
}
