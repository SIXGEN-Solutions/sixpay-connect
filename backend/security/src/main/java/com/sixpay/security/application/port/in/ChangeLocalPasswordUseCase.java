package com.sixpay.security.application.port.in;

/**
 * Changes the authenticated user's own LOCAL password.
 *
 * <p>This is intentionally separate from administrative password reset.</p>
 */
public interface ChangeLocalPasswordUseCase {

    void changePassword(ChangeLocalPasswordCommand command);
}
