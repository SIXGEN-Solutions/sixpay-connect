package com.sixpay.security.local;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalUserAdministrationServiceTest {

    @Test
    void hashesPasswordBeforePersistence() {
        var repository = mock(LocalAuthUserRepository.class);
        var encoder = new BCryptPasswordEncoder(4);
        var service = new LocalUserAdministrationService(
                repository,
                encoder
        );

        when(repository.findByUsernameIgnoreCase("developer"))
                .thenReturn(Optional.empty());
        when(repository.save(any(LocalAuthUserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var user = service.create(
                "developer",
                "Very-Strong-Local-Password",
                "local-developer",
                Set.of(LocalRole.AUDITOR)
        );

        assertThat(user.getPasswordHash())
                .isNotEqualTo("Very-Strong-Local-Password");
        assertThat(
                encoder.matches(
                        "Very-Strong-Local-Password",
                        user.getPasswordHash()
                )
        ).isTrue();
    }
}
