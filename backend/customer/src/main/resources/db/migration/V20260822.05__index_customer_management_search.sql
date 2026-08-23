CREATE INDEX ix_customer_management_customer_financial_institution
    ON customer_management_customer (
                                     financial_institution_code
        );

CREATE INDEX ix_customer_management_customer_created_at
    ON customer_management_customer (
                                     created_at DESC
        );