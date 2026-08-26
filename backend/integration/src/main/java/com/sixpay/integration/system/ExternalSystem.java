package com.sixpay.integration.system;

import com.sixpay.common.validation.Preconditions;

/**
 * Identifies an external system communicating with SIXPAY CONNECT.
 *
 * @param name external system name
 */
public record ExternalSystem(String name) {

    public static final ExternalSystem AMPLITUDE =
            new ExternalSystem("AMPLITUDE");

    public static final ExternalSystem TRESORPAY =
            new ExternalSystem("TRESORPAY");

    public ExternalSystem {
        name = Preconditions.requireNonBlank(
                name,
                "External system name must not be blank"
        );
    }

    public static ExternalSystem of(String name) {
        return new ExternalSystem(name);
    }

    @Override
    public String toString() {
        return name;
    }
}