package com.sixpay.security.application.service;

import com.sixpay.security.application.port.input.GetCurrentSessionUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;

import java.util.Objects;

public final class CurrentSessionService
        implements GetCurrentSessionUseCase {

    private final CurrentUserProvider currentUserProvider;

    public CurrentSessionService(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
    }

    @Override
    public AuthenticatedUser getCurrentSession() {
        return currentUserProvider.requireCurrentUser();
    }
}
