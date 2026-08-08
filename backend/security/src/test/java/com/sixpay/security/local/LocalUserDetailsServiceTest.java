package com.sixpay.security.local;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalUserDetailsServiceTest {

    private final LocalAuthUserRepository repository =
            mock(LocalAuthUserRepository.class);

    private final LocalUserDetailsService service =
            new LocalUserDetailsService(repository);

    @Test
    void loadsSubjectRoleAndScopes() {
        var entity = new LocalAuthUserEntity(
                UUID.randomUUID(),
                "admin",
                "$2a$12$hash",
                "local-admin",
                true,
                Set.of(LocalRole.ADMIN),
                Instant.now(),
                Instant.now()
        );

        when(repository.findByUsernameIgnoreCase("admin"))
                .thenReturn(Optional.of(entity));

        var principal = (LocalPrincipal) service.loadUserByUsername("admin");

        assertThat(principal.subject()).isEqualTo("local-admin");
        assertThat(principal.roles()).containsExactly(LocalRole.ADMIN);
        assertThat(principal.getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_ADMIN", "SCOPE_payment.read");
    }

    @Test
    void hidesWhetherUnknownUsernameExists() {
        when(repository.findByUsernameIgnoreCase("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");
    }
}
