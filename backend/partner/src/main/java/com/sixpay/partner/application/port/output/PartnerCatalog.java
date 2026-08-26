package com.sixpay.partner.application.port.output;

import com.sixpay.partner.application.view.PartnerPage;

public interface PartnerCatalog {

    PartnerPage findPage(int page, int size);
}
