package com.sixpay.security.infrastructure.authentication.persistence;

import com.sixpay.security.application.model.PasswordHistorySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JpaPasswordHistoryAdapterTest {

    private static final UUID USER_ID = UUID.fromString(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    );

    private LocalAuthenticationUserSpringDataRepository localRepository;
    private PasswordHistorySpringDataRepository historyRepository;
    private JpaPasswordHistoryAdapter adapter;

    @BeforeEach
    void setUp() {
        localRepository = mock(LocalAuthenticationUserSpringDataRepository.class);
        historyRepository = mock(PasswordHistorySpringDataRepository.class);
        adapter = new JpaPasswordHistoryAdapter(localRepository, historyRepository);
    }

    @Test
    void loadsCurrentHashAndConfiguredNumberOfRecentHashes() {
        LocalAuthenticationUserJpaEntity local = mock(LocalAuthenticationUserJpaEntity.class);
        when(local.getPasswordHash()).thenReturn("current-hash");
        when(localRepository.findForCredentialUpdate(USER_ID))
                .thenReturn(Optional.of(local));
        when(historyRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(
                        history("old-3", 3),
                        history("old-2", 2),
                        history("old-1", 1)
                ));

        PasswordHistorySnapshot result = adapter.loadForPasswordReplacement(USER_ID, 2);

        assertThat(result.currentPasswordHash()).isEqualTo("current-hash");
        assertThat(result.recentPasswordHashes())
                .containsExactly("old-3", "old-2");
    }

    @Test
    void zeroHistoryStillProtectsCurrentPassword() {
        LocalAuthenticationUserJpaEntity local = mock(LocalAuthenticationUserJpaEntity.class);
        when(local.getPasswordHash()).thenReturn("current-hash");
        when(localRepository.findForCredentialUpdate(USER_ID))
                .thenReturn(Optional.of(local));

        PasswordHistorySnapshot result = adapter.loadForPasswordReplacement(USER_ID, 0);

        assertThat(result.currentPasswordHash()).isEqualTo("current-hash");
        assertThat(result.recentPasswordHashes()).isEmpty();
        verifyNoInteractions(historyRepository);
    }

    @Test
    void archivesReplacedHashAndPrunesEntriesBeyondHistorySize() {
        PasswordHistoryJpaEntity newest = history("newest", 4);
        PasswordHistoryJpaEntity second = history("second", 3);
        PasswordHistoryJpaEntity obsolete = history("obsolete", 2);
        when(historyRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(newest, second, obsolete));

        adapter.archiveReplacedPassword(
                USER_ID,
                "current-hash",
                Instant.parse("2026-08-15T20:00:00Z"),
                2
        );

        verify(historyRepository).save(any(PasswordHistoryJpaEntity.class));
        verify(historyRepository).deleteAll(List.of(obsolete));
    }

    private static PasswordHistoryJpaEntity history(String hash, int seconds) {
        return PasswordHistoryJpaEntity.archived(
                USER_ID,
                hash,
                Instant.parse("2026-08-15T20:00:00Z").plusSeconds(seconds)
        );
    }
}
