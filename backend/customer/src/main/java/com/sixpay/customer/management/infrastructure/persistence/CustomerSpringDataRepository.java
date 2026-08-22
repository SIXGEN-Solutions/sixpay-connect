package com.sixpay.customer.management.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerSpringDataRepository
        extends JpaRepository<CustomerJpaEntity, UUID> {

    @EntityGraph(attributePaths = "bankAccounts")
    @Query(
            "select distinct customer "
                    + "from CustomerJpaEntity customer "
                    + "where customer.id = :customerId"
    )
    Optional<CustomerJpaEntity> findAggregateById(
            @Param("customerId") UUID customerId
    );

    boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(
            String financialInstitutionCode,
            String bankingCustomerReference
    );
}
