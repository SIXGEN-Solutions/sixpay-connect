package com.sixpay.security.application.port.output;

import com.sixpay.security.domain.authentication.LocalAuthenticationUser;

import java.util.Optional;

@FunctionalInterface
public interface LoadAuthenticationUserPort {

    Optional<LocalAuthenticationUser> loadForAuthentication(String normalizedUsername);
}
