package com.sixpay.security.application.port.input;

import com.sixpay.common.validation.Preconditions;

public record LocalLoginCommand(
        String username,
        String password
) {
    public LocalLoginCommand {
        username = Preconditions.requireNonBlank(username, "Username must not be blank");
        password = Preconditions.requireNonBlank(password, "Password must not be blank");
    }
}
