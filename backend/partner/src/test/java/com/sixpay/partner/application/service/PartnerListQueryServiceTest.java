package com.sixpay.partner.application.service;

import com.sixpay.partner.application.port.output.PartnerCatalog;
import com.sixpay.partner.application.view.PartnerPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerListQueryServiceTest {

    @Test
    void delegatesAValidPageRequestToCatalog() {
        PartnerCatalog catalog = (page, size) ->
                new PartnerPage(List.of(), page, size, 0, 0);

        var service = new PartnerListQueryService(catalog);

        var result = service.list(2, 20);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void rejectsNegativePage() {
        var service = new PartnerListQueryService(
                (page, size) ->
                        new PartnerPage(List.of(), page, size, 0, 0)
        );

        assertThatThrownBy(() -> service.list(-1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be zero or positive");
    }

    @Test
    void rejectsPageSizeAboveContractLimit() {
        var service = new PartnerListQueryService(
                (page, size) ->
                        new PartnerPage(List.of(), page, size, 0, 0)
        );

        assertThatThrownBy(() -> service.list(0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 100");
    }
}
