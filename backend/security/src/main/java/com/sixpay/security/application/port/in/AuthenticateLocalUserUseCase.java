package com.sixpay.security.application.port.in;

import com.sixpay.security.authentication.AuthenticatedUser;

@FunctionalInterface
public interface AuthenticateLocalUserUseCase {

    AuthenticatedUser authenticate(LocalLoginCommand command);
}
