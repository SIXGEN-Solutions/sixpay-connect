package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.configuration.CustomerModuleConfiguration;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CustomerRepositoryPostgreSqlIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "com.sixpay.customer.configuration."
                        + "CustomerModuleConfiguration"
        }
)
@Testcontainers
class CustomerRepositoryPostgreSqlIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse(
                            "postgres:15-alpine"
                    )
            );

    @DynamicPropertySource
    static void datasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );
    }

    @Autowired
    private CustomerRepository repository;

    @Test
    void persistsAndReconstitutesCompleteAggregate() {
        Instant now =
                Instant.parse(
                        "2026-08-22T20:00:00Z"
                );

        CustomerId customerId =
                new CustomerId(
                        UUID.randomUUID()
                );

        Customer customer =
                Customer.create(
                        customerId,
                        "SIXPAY_BANK",
                        "BANK-CUSTOMER-001",
                        "000123",
                        "NIU-001",
                        "Customer One",
                        "customer@example.com",
                        "+237600000001",
                        CustomerBankAccount.create(
                                new CustomerBankAccountId(
                                        UUID.randomUUID()
                                ),
                                customerId,
                                "ACC-001",
                                "v1:"
                                        + "a".repeat(64),
                                "****0001",
                                "XAF",
                                "CURRENT",
                                now
                        ),
                        now
                );

        repository.save(customer);

        Customer loaded =
                repository.findById(
                                customerId
                        )
                        .orElseThrow();

        assertThat(
                loaded.id()
        ).isEqualTo(
                customerId
        );

        assertThat(
                loaded.legalName()
        ).isEqualTo(
                "Customer One"
        );

        assertThat(
                loaded.bankAccounts()
        )
                .singleElement()
                .satisfies(account -> {
                    assertThat(
                            account
                                    .bankingAccountReference()
                    ).isEqualTo(
                            "ACC-001"
                    );

                    assertThat(
                            account.defaultAccount()
                    ).isTrue();
                });

        assertThat(
                repository
                        .existsByFinancialInstitutionCodeAndBankingCustomerReference(
                                "SIXPAY_BANK",
                                "BANK-CUSTOMER-001"
                        )
        ).isTrue();
    }

    @Test
    void synchronizesBankAccountsAndStatus() {
        Instant now =
                Instant.parse(
                        "2026-08-22T20:00:00Z"
                );

        CustomerId customerId =
                new CustomerId(
                        UUID.randomUUID()
                );

        Customer customer =
                Customer.create(
                        customerId,
                        "SIXPAY_BANK",
                        "BANK-CUSTOMER-002",
                        "000124",
                        "NIU-002",
                        "Customer Two",
                        null,
                        null,
                        CustomerBankAccount.create(
                                new CustomerBankAccountId(
                                        UUID.randomUUID()
                                ),
                                customerId,
                                "ACC-010",
                                "v1:"
                                        + "b".repeat(64),
                                "****0010",
                                "XAF",
                                "CURRENT",
                                now
                        ),
                        now
                );

        repository.save(customer);

        CustomerBankAccount second =
                CustomerBankAccount.create(
                        new CustomerBankAccountId(
                                UUID.randomUUID()
                        ),
                        customerId,
                        "ACC-011",
                        "v1:"
                                + "c".repeat(64),
                        "****0011",
                        "XAF",
                        "SAVINGS",
                        now.plusSeconds(1)
                );

        customer.addBankAccount(
                second,
                now.plusSeconds(1)
        );

        customer.makeDefaultBankAccount(
                second.id(),
                now.plusSeconds(2)
        );

        customer.suspend(
                "manual review",
                now.plusSeconds(3)
        );

        repository.save(customer);

        Customer loaded =
                repository.findById(
                                customerId
                        )
                        .orElseThrow();

        assertThat(
                loaded.status().name()
        ).isEqualTo(
                "SUSPENDED"
        );

        assertThat(
                loaded.statusReason()
        ).contains(
                "manual review"
        );

        assertThat(
                loaded.bankAccounts()
        ).hasSize(2);

        assertThat(
                loaded
                        .defaultBankAccount()
                        .orElseThrow()
                        .bankingAccountReference()
        ).isEqualTo(
                "ACC-011"
        );
    }

    @Test
    void searchWithoutOptionalFiltersWorksOnPostgreSql() {
        Instant now =
                Instant.parse(
                        "2026-08-23T05:00:00Z"
                );

        Customer customer =
                createSearchCustomer(
                        "NOFILTER",
                        "NIU-NOFILTER",
                        "Customer No Filter",
                        "BANK_NO_FILTER",
                        now
                );

        repository.save(customer);

        var page = repository.search(
                new CustomerSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        0,
                        100
                )
        );

        assertThat(
                page.content()
                        .stream()
                        .map(Customer::id)
        ).contains(
                customer.id()
        );
    }

    @Test
    void searchAppliesCustomerMasterFilters() {
        Instant now =
                Instant.parse(
                        "2026-08-23T05:05:00Z"
                );

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8)
                        .toUpperCase();

        Customer customer =
                createSearchCustomer(
                        suffix,
                        "NIU-" + suffix,
                        "Alpha Search " + suffix,
                        "BANK_" + suffix,
                        now
                );

        repository.save(customer);

        var byNiu = repository.search(
                new CustomerSearchCriteria(
                        suffix.toLowerCase(),
                        null,
                        null,
                        null,
                        0,
                        20
                )
        );

        assertThat(
                byNiu.content()
                        .stream()
                        .map(Customer::id)
        ).contains(
                customer.id()
        );

        var byName = repository.search(
                new CustomerSearchCriteria(
                        null,
                        "alpha search "
                                + suffix.toLowerCase(),
                        null,
                        null,
                        0,
                        20
                )
        );

        assertThat(
                byName.content()
                        .stream()
                        .map(Customer::id)
        ).contains(
                customer.id()
        );

        var byInstitutionAndStatus = repository.search(
                new CustomerSearchCriteria(
                        null,
                        null,
                        customer.status(),
                        ("BANK_" + suffix)
                                .toLowerCase(),
                        0,
                        20
                )
        );

        assertThat(
                byInstitutionAndStatus.content()
                        .stream()
                        .map(Customer::id)
        ).contains(
                customer.id()
        );
    }

    private Customer createSearchCustomer(
            String suffix,
            String niu,
            String legalName,
            String financialInstitutionCode,
            Instant now
    ) {
        CustomerId customerId =
                new CustomerId(
                        UUID.randomUUID()
                );

        return Customer.create(
                customerId,
                financialInstitutionCode,
                "BANK-CUSTOMER-" + suffix,
                "NUM-" + suffix,
                niu,
                legalName,
                "search-" + suffix.toLowerCase()
                        + "@example.com",
                "+237600000000",
                CustomerBankAccount.create(
                        new CustomerBankAccountId(
                                UUID.randomUUID()
                        ),
                        customerId,
                        "ACC-" + suffix,
                        "v1:" + "d".repeat(64),
                        "****"
                                + suffix.substring(
                                        Math.max(
                                                0,
                                                suffix.length() - 4
                                        )
                                ),
                        "XAF",
                        "CURRENT",
                        now
                ),
                now
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = CustomerModuleConfiguration.class
    )
    @EntityScan(
            basePackageClasses =
                    CustomerJpaEntity.class
    )
    @EnableJpaRepositories(
            basePackageClasses =
                    CustomerSpringDataRepository.class
    )
    @Import(
            CustomerRepositoryAdapter.class
    )
    static class TestApplication {
    }
}