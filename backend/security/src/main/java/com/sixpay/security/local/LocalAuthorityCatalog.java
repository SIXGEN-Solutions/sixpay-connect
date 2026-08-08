package com.sixpay.security.local;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.LinkedHashSet;
import java.util.Set;

public final class LocalAuthorityCatalog {

    private LocalAuthorityCatalog() {
    }

    public static Set<SimpleGrantedAuthority> authoritiesFor(LocalRole role) {
        var authorities = new LinkedHashSet<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        switch (role) {
            case ADMIN -> {
                readAuthorities(authorities);
                authorities.add(scope("partner.write"));
                authorities.add(scope("partner.admin"));
            }
            case MANAGER -> {
                readAuthorities(authorities);
                authorities.add(scope("partner.validate"));
                authorities.add(scope("partner.lifecycle"));
            }
            case AUDITOR -> {
                readAuthorities(authorities);
                authorities.add(scope("payment.audit.read"));
                authorities.add(scope("payment.audit.export"));
            }
            case PARTNER -> {
                authorities.add(scope("partner.self.read"));
            }
        }

        return Set.copyOf(authorities);
    }

    private static void readAuthorities(Set<SimpleGrantedAuthority> authorities) {
        authorities.add(scope("payment.read"));
        authorities.add(scope("observed-customer.read"));
        authorities.add(scope("partner.read"));
    }

    private static SimpleGrantedAuthority scope(String value) {
        return new SimpleGrantedAuthority("SCOPE_" + value);
    }
}
