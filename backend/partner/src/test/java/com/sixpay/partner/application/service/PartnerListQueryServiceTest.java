package com.sixpay.partner.application.service;

import com.sixpay.partner.application.port.output.PartnerCatalog;
import com.sixpay.partner.application.view.PartnerPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerListQueryServiceTest {

    @Test
    void delegatesPageZeroToCatalog() {
        PartnerCatalog catalog = (page, size) ->
                new PartnerPage(List.of(), page, size, 41, 3);

        var result = new PartnerListQueryService(catalog)
                .list(0, 20);

        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(41);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void delegatesPageOneWithoutResettingPagination() {
        PartnerCatalog catalog = (page, size) ->
                new PartnerPage(List.of(), page, size, 41, 3);

        var result = new PartnerListQueryService(catalog)
                .list(1, 20);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void acceptsTheMaximumContractPageSize() {
        PartnerCatalog catalog = (page, size) ->
                new PartnerPage(List.of(), page, size, 0, 0);

        var result = new PartnerListQueryService(catalog)
                .list(0, 100);

        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void rejectsNegativePage() {
        var service = service();

        assertThatThrownBy(() -> service.list(-1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be zero or positive");
    }

    @Test
    void rejectsZeroPageSize() {
        var service = service();

        assertThatThrownBy(() -> service.list(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 100");
    }

    @Test
    void rejectsPageSizeAboveContractLimit() {
        var service = service();

        assertThatThrownBy(() -> service.list(0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 100");
    }

    private static PartnerListQueryService service() {
        return new PartnerListQueryService(
                (page, size) ->
                        new PartnerPage(List.of(), page, size, 0, 0)
        );
    }
}
