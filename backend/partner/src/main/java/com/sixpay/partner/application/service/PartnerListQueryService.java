package com.sixpay.partner.application.service;

import com.sixpay.partner.application.port.in.PartnerListQueryUseCase;
import com.sixpay.partner.application.port.output.PartnerCatalog;
import com.sixpay.partner.application.view.PartnerPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class PartnerListQueryService implements PartnerListQueryUseCase {

    private final PartnerCatalog catalog;

    public PartnerListQueryService(PartnerCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog);
    }

    @Override
    public PartnerPage list(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be zero or positive"
            );
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
        return catalog.findPage(page, size);
    }
}
