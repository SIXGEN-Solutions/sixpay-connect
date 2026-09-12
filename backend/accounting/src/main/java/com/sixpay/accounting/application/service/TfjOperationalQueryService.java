package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.TfjOperationalConfirmationNotFoundException;
import com.sixpay.accounting.application.port.input.TfjOperationalQueryUseCase;
import com.sixpay.accounting.application.port.output.TfjConfirmationRepository;
import com.sixpay.accounting.domain.model.TfjOperationalCategory;
import com.sixpay.accounting.domain.model.TfjOperationalSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TfjOperationalQueryService implements TfjOperationalQueryUseCase {

    private final TfjConfirmationRepository repository;

    public TfjOperationalQueryService(
            TfjConfirmationRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Page search(
            LocalDate businessDate,
            TfjOperationalCategory category,
            String paymentReference,
            String bankPostingReference,
            int page,
            int size
    ) {
        validatePagination(page, size);

        var result = repository.searchOperational(
                businessDate,
                normalize(paymentReference),
                normalize(bankPostingReference),
                page,
                size
        );

        List<TfjOperationalSnapshot> snapshots = result.content().stream()
                .map(TfjOperationalSnapshot::from)
                .filter(snapshot -> category == null || snapshot.category() == category)
                .toList();

        if (category == null) {
            return new Page(
                    snapshots,
                    page,
                    size,
                    result.totalElements()
            );
        }

        var allMatching = repository.searchOperationalAll(
                        businessDate,
                        normalize(paymentReference),
                        normalize(bankPostingReference)
                )
                .stream()
                .map(TfjOperationalSnapshot::from)
                .filter(snapshot -> snapshot.category() == category)
                .toList();

        long start = (long) page * size;
        List<TfjOperationalSnapshot> content;

        if (start >= allMatching.size()) {
            content = List.of();
        } else {
            int fromIndex = (int) start;
            int toIndex = Math.min(fromIndex + size, allMatching.size());
            content = allMatching.subList(fromIndex, toIndex);
        }

        return new Page(
                content,
                page,
                size,
                allMatching.size()
        );
    }

    @Override
    public TfjOperationalSnapshot findByConfirmationId(UUID confirmationId) {
        return repository.findByConfirmationId(
                        Objects.requireNonNull(confirmationId)
                )
                .map(TfjOperationalSnapshot::from)
                .orElseThrow(
                        () -> new TfjOperationalConfirmationNotFoundException(
                                confirmationId
                        )
                );
    }

    private static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
