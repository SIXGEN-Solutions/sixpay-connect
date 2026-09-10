package com.sixpay.accounting.infrastructure.tfj.persistence;

import com.sixpay.accounting.application.port.output.TfjMatchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class AccountingTfjMatchRepositoryAdapter
        implements TfjMatchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UUID> findPaymentIds(
            String financialInstitutionCode,
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference
    ) {
        @SuppressWarnings("unchecked")
        List<UUID> ids =
                entityManager.createNativeQuery(
                                "select i.payment_id "
                                        + "from accounting_batch_items i "
                                        + "join accounting_batches b "
                                        + "on b.id = i.batch_id "
                                        + "where b.financial_institution_code = :financialInstitutionCode "
                                        + "and b.business_date = :businessDate "
                                        + "and i.public_payment_reference = :paymentReference "
                                        + "and i.bank_posting_reference = :bankPostingReference"
                        )
                        .setParameter(
                                "financialInstitutionCode",
                                financialInstitutionCode
                        )
                        .setParameter(
                                "businessDate",
                                businessDate
                        )
                        .setParameter(
                                "paymentReference",
                                paymentReference
                        )
                        .setParameter(
                                "bankPostingReference",
                                bankPostingReference
                        )
                        .getResultList();

        return List.copyOf(ids);
    }
}
