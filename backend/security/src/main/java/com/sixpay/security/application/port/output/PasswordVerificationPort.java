package com.sixpay.security.application.port.output;

public interface PasswordVerificationPort {

    boolean matches(CharSequence rawPassword, String passwordHash);

    void performDummyVerification(CharSequence rawPassword);
}
