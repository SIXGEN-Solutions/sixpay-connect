package com.sixpay.security.infrastructure.authentication.password;

import com.sixpay.security.application.port.out.PasswordVerificationPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

public final class BCryptPasswordVerificationAdapter
        implements PasswordVerificationPort {

    private static final String DUMMY_PASSWORD =
            "sixpay-local-authentication-dummy-password";

    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public BCryptPasswordVerificationAdapter(
            PasswordEncoder passwordEncoder
    ) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.dummyHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Override
    public boolean matches(
            CharSequence rawPassword,
            String passwordHash
    ) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Override
    public void performDummyVerification(
            CharSequence rawPassword
    ) {
        passwordEncoder.matches(rawPassword, dummyHash);
    }
}
