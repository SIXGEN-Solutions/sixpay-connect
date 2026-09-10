package com.sixpay.administration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneralParameterSpringDataRepository
        extends JpaRepository<GeneralParameterJpaEntity, String> {
}
