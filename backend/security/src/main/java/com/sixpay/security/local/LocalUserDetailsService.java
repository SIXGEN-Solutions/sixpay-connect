package com.sixpay.security.local;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final LocalAuthUserRepository repository;

    public LocalUserDetailsService(LocalAuthUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var user = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid credentials")
                );

        var authorities = new LinkedHashSet<SimpleGrantedAuthority>();
        user.getRoles().forEach(role ->
                authorities.addAll(LocalAuthorityCatalog.authoritiesFor(role))
        );

        return new LocalPrincipal(
                user.getUsername(),
                user.getPasswordHash(),
                user.getSubject(),
                user.isEnabled(),
                user.getRoles(),
                Set.copyOf(authorities)
        );
    }
}
