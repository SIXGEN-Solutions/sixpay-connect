package com.sixpay.security.application.port.in;

import com.sixpay.security.authentication.AuthenticatedUser;

@FunctionalInterface
public interface LogoutUseCase {

    void logout(AuthenticatedUser currentUser);
}
