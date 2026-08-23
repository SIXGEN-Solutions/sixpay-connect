package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(
            "select customer "
                    + "from CustomerJpaEntity customer "
                    + "where (:niu is null "
                    + "or lower(customer.niu) "
                    + "like lower(concat('%', :niu, '%'))) "
                    + "and (:legalName is null "
                    + "or lower(customer.legalName) "
                    + "like lower(concat('%', :legalName, '%'))) "
                    + "and (:status is null "
                    + "or customer.status = :status) "
                    + "and (:financialInstitutionCode is null "
                    + "or lower(customer.financialInstitutionCode) "
                    + "= lower(:financialInstitutionCode))"
    )
    Page<CustomerJpaEntity> search(
            @Param("niu") String niu,
            @Param("legalName") String legalName,
            @Param("status") CustomerStatus status,
            @Param("financialInstitutionCode")
            String financialInstitutionCode,
            Pageable pageable
    );

    boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(
            String financialInstitutionCode,
            String bankingCustomerReference
    );
}
