package com.sixpay.partner.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PartnerSpringDataRepository extends JpaRepository<PartnerJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"authorizedTransactionTypes", "validationThresholds"})
    @Query("select distinct partner from PartnerJpaEntity partner where partner.id = :partnerId")
    Optional<PartnerJpaEntity> findAggregateById(@Param("partnerId") UUID partnerId);
}
