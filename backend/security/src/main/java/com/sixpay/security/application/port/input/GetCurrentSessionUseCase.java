package com.sixpay.security.application.port.input;

import com.sixpay.security.authentication.AuthenticatedUser;

@FunctionalInterface
public interface GetCurrentSessionUseCase {

    AuthenticatedUser getCurrentSession();
}
