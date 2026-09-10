package com.sixpay.administration.application.service;

import com.sixpay.administration.application.port.input.GeneralParameterQueryUseCase;
import com.sixpay.administration.application.port.output.GeneralParameterRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class GeneralParameterQueryService
        implements GeneralParameterQueryUseCase {

    private final GeneralParameterRepositoryPort repository;

    public GeneralParameterQueryService(
            GeneralParameterRepositoryPort repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public String requireValue(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            throw new IllegalArgumentException("typeCode is required");
        }

        return repository.findByTypeCode(typeCode.strip())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Missing SIXPAY general parameter: "
                                        + typeCode.strip()
                        )
                )
                .valeurCode();
    }
}
