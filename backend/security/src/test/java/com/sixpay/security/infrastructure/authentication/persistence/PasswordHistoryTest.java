package com.sixpay.security.infrastructure.authentication.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PasswordHistoryTest {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private static final Instant NOW =
            Instant.parse(
                    "2026-08-16T02:00:00Z"
            );

    private LocalAuthenticationUserSpringDataRepository
            localRepository;

    private PasswordHistorySpringDataRepository
            historyRepository;

    private JpaPasswordHistoryAdapter adapter;

    @BeforeEach
    void setUp() {
        localRepository =
                mock(
                        LocalAuthenticationUserSpringDataRepository.class
                );

        historyRepository =
                mock(
                        PasswordHistorySpringDataRepository.class
                );

        adapter =
                new JpaPasswordHistoryAdapter(
                        localRepository,
                        historyRepository
                );
    }

    @Test
    void loadsCurrentCredentialAndOnlyConfiguredRecentHistory() {
        LocalAuthenticationUserJpaEntity local =
                mock(
                        LocalAuthenticationUserJpaEntity.class
                );

        when(local.getPasswordHash())
                .thenReturn("current-hash");

        when(
                localRepository.findForCredentialUpdate(
                        USER_ID
                )
        )
                .thenReturn(
                        Optional.of(local)
                );

        var newest =
                PasswordHistoryJpaEntity.archived(
                        USER_ID,
                        "history-3",
                        NOW
                );

        var previous =
                PasswordHistoryJpaEntity.archived(
                        USER_ID,
                        "history-2",
                        NOW.minusSeconds(60)
                );

        var oldest =
                PasswordHistoryJpaEntity.archived(
                        USER_ID,
                        "history-1",
                        NOW.minusSeconds(120)
                );

        when(
                historyRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                USER_ID
                        )
        )
                .thenReturn(
                        List.of(
                                newest,
                                previous,
                                oldest
                        )
                );

        var snapshot =
                adapter.loadForPasswordReplacement(
                        USER_ID,
                        2
                );

        assertThat(
                snapshot.currentPasswordHash()
        )
                .isEqualTo(
                        "current-hash"
                );

        assertThat(
                snapshot.recentPasswordHashes()
        )
                .containsExactly(
                        "history-3",
                        "history-2"
                );
    }

    @Test
    void historySizeZeroStillLoadsCurrentCredentialWithoutReadingHistoryRows() {
        LocalAuthenticationUserJpaEntity local =
                mock(
                        LocalAuthenticationUserJpaEntity.class
                );

        when(local.getPasswordHash())
                .thenReturn("current-hash");

        when(
                localRepository.findForCredentialUpdate(
                        USER_ID
                )
        )
                .thenReturn(
                        Optional.of(local)
                );

        var snapshot =
                adapter.loadForPasswordReplacement(
                        USER_ID,
                        0
                );

        assertThat(
                snapshot.currentPasswordHash()
        )
                .isEqualTo(
                        "current-hash"
                );

        assertThat(
                snapshot.recentPasswordHashes()
        )
                .isEmpty();

        verifyNoInteractions(
                historyRepository
        );
    }

    @Test
    void archivesReplacedHashAndPrunesRowsBeyondConfiguredHistorySize() {
        var first =
                PasswordHistoryJpaEntity.archived(
                        USER_ID,
                        "newly-archived",
                        NOW
                );

        var second =
                PasswordHistoryJpaEntity.archived(
                        USER_ID,
                        "history-2",
                        NOW.minusSeconds(60)
                );

        var third =
                PasswordHistoryJpaEntity.archived(
                        USER_ID,
                        "history-1",
                        NOW.minusSeconds(120)
                );

        when(
                historyRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                USER_ID
                        )
        )
                .thenReturn(
                        List.of(
                                first,
                                second,
                                third
                        )
                );

        adapter.archiveReplacedPassword(
                USER_ID,
                "current-hash",
                NOW,
                2
        );

        ArgumentCaptor<PasswordHistoryJpaEntity> archived =
                ArgumentCaptor.forClass(
                        PasswordHistoryJpaEntity.class
                );

        verify(historyRepository)
                .save(
                        archived.capture()
                );

        assertThat(
                archived.getValue()
                        .getUserId()
        )
                .isEqualTo(USER_ID);

        assertThat(
                archived.getValue()
                        .getPasswordHash()
        )
                .isEqualTo(
                        "current-hash"
                );

        assertThat(
                archived.getValue()
                        .getCreatedAt()
        )
                .isEqualTo(NOW);

        verify(historyRepository)
                .deleteAll(
                        List.of(third)
                );
    }

    @Test
    void failsClosedWhenLocalCredentialIsNotProvisioned() {
        when(
                localRepository.findForCredentialUpdate(
                        USER_ID
                )
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThatThrownBy(() ->
                adapter.loadForPasswordReplacement(
                        USER_ID,
                        5
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Local authentication is not provisioned"
                );

        verifyNoInteractions(
                historyRepository
        );
    }

    @Test
    void rejectsNegativeHistorySize() {
        assertThatThrownBy(() ->
                adapter.loadForPasswordReplacement(
                        USER_ID,
                        -1
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                );

        assertThatThrownBy(() ->
                adapter.archiveReplacedPassword(
                        USER_ID,
                        "hash",
                        NOW,
                        -1
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                );
    }
}
