package com.sixpay.accounting.infrastructure.tfj.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sixpay.accounting.domain.model.TfjStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountingTfjConfirmationSpringDataRepository
        extends JpaRepository<
                AccountingTfjConfirmationJpaEntity,
                UUID
        > {

    Optional<AccountingTfjConfirmationJpaEntity>
    findByIdempotencyKey(String idempotencyKey);

    @Query(
            value = """
                    select c from AccountingTfjConfirmationJpaEntity c
                    where (:businessDate is null or c.businessDate = :businessDate)
                      and (
                            :paymentReference is null
                            or lower(c.paymentReference)
                               like lower(concat('%', :paymentReference, '%'))
                          )
                      and (
                            :bankPostingReference is null
                            or lower(c.bankPostingReference)
                               like lower(concat('%', :bankPostingReference, '%'))
                          )
                    order by c.receivedAt desc, c.confirmationId asc
                    """,
            countQuery = """
                    select count(c) from AccountingTfjConfirmationJpaEntity c
                    where (:businessDate is null or c.businessDate = :businessDate)
                      and (
                            :paymentReference is null
                            or lower(c.paymentReference)
                               like lower(concat('%', :paymentReference, '%'))
                          )
                      and (
                            :bankPostingReference is null
                            or lower(c.bankPostingReference)
                               like lower(concat('%', :bankPostingReference, '%'))
                          )
                    """
    )
    Page<AccountingTfjConfirmationJpaEntity> searchOperational(
            @Param("businessDate") LocalDate businessDate,
            @Param("paymentReference") String paymentReference,
            @Param("bankPostingReference") String bankPostingReference,
            Pageable pageable
    );

    @Query("""
            select c from AccountingTfjConfirmationJpaEntity c
            where (:businessDate is null or c.businessDate = :businessDate)
              and (
                    :paymentReference is null
                    or lower(c.paymentReference)
                       like lower(concat('%', :paymentReference, '%'))
                  )
              and (
                    :bankPostingReference is null
                    or lower(c.bankPostingReference)
                       like lower(concat('%', :bankPostingReference, '%'))
                  )
            order by c.receivedAt desc, c.confirmationId asc
            """)
    List<AccountingTfjConfirmationJpaEntity> searchOperationalAll(
            @Param("businessDate") LocalDate businessDate,
            @Param("paymentReference") String paymentReference,
            @Param("bankPostingReference") String bankPostingReference
    );

    List<AccountingTfjConfirmationJpaEntity>
    findByMatchedPaymentIdIsNotNullAndFinalityPublishedAtIsNullOrderByReceivedAtAsc(
            Pageable pageable
    );

    long countByMatchedPaymentIdIsNotNullAndFinalityPublishedAtIsNullAndStatusIn(
            Collection<TfjStatus> statuses
    );
}
