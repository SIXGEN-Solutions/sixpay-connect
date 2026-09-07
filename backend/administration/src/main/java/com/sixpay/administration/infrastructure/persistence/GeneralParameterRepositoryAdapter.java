package com.sixpay.administration.infrastructure.persistence;

import com.sixpay.administration.application.port.output.GeneralParameterRepositoryPort;
import com.sixpay.administration.domain.model.GeneralParameter;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GeneralParameterRepositoryAdapter
        implements GeneralParameterRepositoryPort {

    private final GeneralParameterSpringDataRepository repository;

    public GeneralParameterRepositoryAdapter(
            GeneralParameterSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Optional<GeneralParameter> findByTypeCode(String typeCode) {
        return repository.findById(typeCode)
                .map(entity ->
                        new GeneralParameter(
                                entity.getTypeCode(),
                                entity.getValeurCode(),
                                entity.getDescription()
                        )
                );
    }
}
