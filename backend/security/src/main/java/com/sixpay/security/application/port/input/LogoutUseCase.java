package com.sixpay.security.application.port.input;

import com.sixpay.security.authentication.AuthenticatedUser;

@FunctionalInterface
public interface LogoutUseCase {

    void logout(AuthenticatedUser currentUser);
}
