package com.sixpay.administration.application.port.output;

import com.sixpay.administration.domain.model.IntegrationStatus;

import java.util.List;

public interface IntegrationHealthQueryPort {

    List<IntegrationStatus> findAll();
}
