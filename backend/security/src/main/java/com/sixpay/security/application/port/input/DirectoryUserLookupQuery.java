package com.sixpay.security.application.port.input;

public record DirectoryUserLookupQuery(
        String username
) {

    public DirectoryUserLookupQuery {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Directory username must not be blank"
            );
        }
        username = username.trim();
        if (username.length() > 150) {
            throw new IllegalArgumentException(
                    "Directory username must contain at most 150 characters"
            );
        }
    }
}
