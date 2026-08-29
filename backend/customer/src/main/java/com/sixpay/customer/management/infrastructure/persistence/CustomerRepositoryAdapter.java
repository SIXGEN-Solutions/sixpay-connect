package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;
import com.sixpay.customer.management.domain.repository.CustomerSearchPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerSpringDataRepository repository;

    public CustomerRepositoryAdapter(
            CustomerSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        CustomerJpaEntity entity =
                repository.findAggregateById(
                                customer.id().value()
                        )
                        .orElse(null);

        if (entity == null) {
            repository.save(
                    CustomerJpaEntity.create(
                            customer
                    )
            );

            return customer;
        }

        /*
         * Important:
         *
         * PostgreSQL enforces exactly one default account
         * through a partial unique index.
         *
         * Hibernate normally executes INSERTs before UPDATEs
         * during flush. When a newly-added account becomes
         * the default account, inserting it before demoting
         * the existing default would temporarily violate
         * the unique index.
         *
         * Therefore the switch is intentionally persisted
         * input two phases.
         */
        boolean defaultAccountChanged =
                entity.prepareDefaultAccountSwitch(
                        customer
                );

        if (defaultAccountChanged) {
            repository.saveAndFlush(entity);
        }

        entity.synchronize(customer);

        repository.save(entity);

        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return repository.findAggregateById(
                        customerId.value()
                )
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSearchPage search(
            CustomerSearchCriteria criteria
    ) {
        var pageable = PageRequest.of(
                criteria.page(),
                criteria.size(),
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        var result = repository.findAll(
                buildSearchSpecification(
                        criteria
                ),
                pageable
        );

        var content = result.getContent()
                .stream()
                .map(this::toDomain)
                .toList();

        return new CustomerSearchPage(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Override
    public boolean existsById(CustomerId customerId) {
        return repository.existsById(customerId.value());
    }

    @Override
    public boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(
            String financialInstitutionCode,
            String bankingCustomerReference
    ) {
        return repository
                .existsByFinancialInstitutionCodeAndBankingCustomerReference(
                        financialInstitutionCode,
                        bankingCustomerReference
                );
    }

    private static Specification<CustomerJpaEntity>
    buildSearchSpecification(
            CustomerSearchCriteria criteria
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates =
                    new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (criteria.niu() != null) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("niu")
                                ),
                                "%"
                                        + criteria.niu()
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                                        + "%"
                        )
                );
            }

            if (criteria.legalName() != null) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("legalName")
                                ),
                                "%"
                                        + criteria.legalName()
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                                        + "%"
                        )
                );
            }

            if (criteria.status() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                criteria.status()
                        )
                );
            }

            if (criteria.financialInstitutionCode() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(
                                        root.get(
                                                "financialInstitutionCode"
                                        )
                                ),
                                criteria.financialInstitutionCode()
                                        .toLowerCase(
                                                Locale.ROOT
                                        )
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            jakarta.persistence.criteria.Predicate[]::new
                    )
            );
        };
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        CustomerId customerId = new CustomerId(entity.id());

        return Customer.reconstitute(
                customerId,
                entity.financialInstitutionCode(),
                entity.bankingCustomerReference(),
                entity.customerNumber(),
                entity.niu(),
                entity.legalName(),
                entity.email(),
                entity.phoneNumber(),
                entity.status(),
                entity.statusReason(),
                entity.createdAt(),
                entity.updatedAt(),
                entity.bankAccounts()
                        .stream()
                        .map(account ->
                                CustomerBankAccount.reconstitute(
                                        new CustomerBankAccountId(
                                                account.id()
                                        ),
                                        customerId,
                                        account.bankingAccountReference(),
                                        account.accountBindingFingerprint(),
                                        account.maskedAccountIdentifier(),
                                        account.currency(),
                                        account.accountType(),
                                        account.defaultAccount(),
                                        account.verifiedAt()
                                )
                        )
                        .toList()
        );
    }
}
