package com.sixpay.partner.application.port.in;

import com.sixpay.partner.application.view.PartnerPage;

public interface PartnerListQueryUseCase {

    int DEFAULT_PAGE_SIZE = 20;
    int MAX_PAGE_SIZE = 100;

    PartnerPage list(int page, int size);
}
