package com.sixpay.security.application.port.in;

import com.sixpay.security.authentication.AuthenticatedUser;

@FunctionalInterface
public interface GetCurrentSessionUseCase {

    AuthenticatedUser getCurrentSession();
}
