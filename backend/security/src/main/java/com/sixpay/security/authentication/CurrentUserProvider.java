package com.sixpay.security.authentication;

import org.springframework.security.authentication
        .AuthenticationCredentialsNotFoundException;

import java.util.Optional;

/**
 * Provides access to the current authenticated user.
 */
public interface CurrentUserProvider {

    Optional<AuthenticatedUser> currentUser();

    default AuthenticatedUser requireCurrentUser() {
        return currentUser().orElseThrow(
                () -> new AuthenticationCredentialsNotFoundException(
                        "No authenticated user is available"
                )
        );
    }
}