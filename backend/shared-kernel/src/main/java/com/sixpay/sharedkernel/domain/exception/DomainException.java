package com.sixpay.sharedkernel.domain.exception;

import com.sixpay.common.validation.Preconditions;

/**
 * Base exception for expected domain rule violations.
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(Preconditions.requireNonBlank(
                message,
                "Domain exception message must not be blank"
        ));

        this.code = Preconditions.requireNonBlank(
                code,
                "Domain exception code must not be blank"
        );
    }

    public DomainException(
            String code,
            String message,
            Throwable cause
    ) {
        super(
                Preconditions.requireNonBlank(
                        message,
                        "Domain exception message must not be blank"
                ),
                cause
        );

        this.code = Preconditions.requireNonBlank(
                code,
                "Domain exception code must not be blank"
        );
    }

    public String code() {
        return code;
    }
}