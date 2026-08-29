package com.sixpay.security.application.port.output;

import com.sixpay.security.domain.authentication.LocalAuthenticationUser;

@FunctionalInterface
public interface SaveAuthenticationUserStatePort {

    void saveAuthenticationState(LocalAuthenticationUser user);
}
