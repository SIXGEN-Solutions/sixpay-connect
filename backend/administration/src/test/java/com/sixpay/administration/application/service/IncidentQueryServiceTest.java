package com.sixpay.administration.application.service;

import com.sixpay.administration.domain.exception.IncidentNotFoundException;
import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;
import com.sixpay.administration.domain.model.OperationalIncident;
import com.sixpay.administration.domain.repository.IncidentSearchCriteria;
import com.sixpay.administration.domain.repository.IncidentSearchPage;
import com.sixpay.administration.domain.repository.OperationalIncidentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentQueryServiceTest {

    @Test
    void delegatesSearchToRepository() {
        OperationalIncident incident =
                incident("INC-001");

        IncidentSearchPage expected =
                new IncidentSearchPage(
                        List.of(incident),
                        1,
                        1,
                        0,
                        20,
                        true,
                        true
                );

        OperationalIncidentRepository repository =
                new StubRepository(
                        Optional.of(incident),
                        expected
                );

        IncidentQueryService service =
                new IncidentQueryService(repository);

        IncidentSearchCriteria criteria =
                new IncidentSearchCriteria(
                        IncidentSeverity.HIGH,
                        IncidentStatus.OPEN,
                        "Accounting",
                        0,
                        20
                );

        assertThat(service.search(criteria))
                .isEqualTo(expected);
    }

    @Test
    void returnsIncidentDetail() {
        OperationalIncident incident =
                incident("INC-002");

        IncidentQueryService service =
                new IncidentQueryService(
                        new StubRepository(
                                Optional.of(incident),
                                emptyPage()
                        )
                );

        assertThat(
                service.get(
                        new IncidentId("INC-002")
                )
        ).isEqualTo(incident);
    }

    @Test
    void failsWhenIncidentDoesNotExist() {
        IncidentQueryService service =
                new IncidentQueryService(
                        new StubRepository(
                                Optional.empty(),
                                emptyPage()
                        )
                );

        assertThatThrownBy(
                () ->
                        service.get(
                                new IncidentId(
                                        "INC-MISSING"
                                )
                        )
        ).isInstanceOf(
                IncidentNotFoundException.class
        );
    }

    private static OperationalIncident incident(
            String id
    ) {
        Instant now =
                Instant.parse(
                        "2026-08-23T14:00:00Z"
                );

        return new OperationalIncident(
                new IncidentId(id),
                IncidentSeverity.HIGH,
                "Accounting",
                "Accounting batch delayed",
                IncidentStatus.OPEN,
                "Batch processing exceeded normal duration",
                "Accounting finalization delayed",
                null,
                null,
                null,
                null,
                now,
                now,
                List.of()
        );
    }

    private static IncidentSearchPage emptyPage() {
        return new IncidentSearchPage(
                List.of(),
                0,
                0,
                0,
                20,
                true,
                true
        );
    }

    private record StubRepository(
            Optional<OperationalIncident> detail,
            IncidentSearchPage page
    ) implements OperationalIncidentRepository {

        @Override
        public Optional<OperationalIncident> findById(
                IncidentId incidentId
        ) {
            return detail;
        }

        @Override
        public IncidentSearchPage search(
                IncidentSearchCriteria criteria
        ) {
            return page;
        }
    }
}
