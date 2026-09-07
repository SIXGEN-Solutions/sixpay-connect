package com.sixpay.administration.application.port.output;

import com.sixpay.administration.domain.model.GeneralParameter;

import java.util.Optional;

public interface GeneralParameterRepositoryPort {
    Optional<GeneralParameter> findByTypeCode(String typeCode);
}
