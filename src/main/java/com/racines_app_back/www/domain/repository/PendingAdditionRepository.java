package com.racines_app_back.www.domain.repository;

import com.racines_app_back.www.domain.entity.PendingAddition;
import com.racines_app_back.www.domain.enums.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingAdditionRepository extends JpaRepository<PendingAddition, UUID> {
    List<PendingAddition> findByStatus(ValidationStatus status);
    List<PendingAddition> findByRequestedBy(UUID userId);
    List<PendingAddition> findByPersonId(UUID personId);
    
    @Query("SELECT pa FROM PendingAddition pa WHERE pa.requestedBy = :userId AND pa.status = :status")
    List<PendingAddition> findByRequestedByAndStatus(
        @Param("userId") UUID userId, 
        @Param("status") ValidationStatus status
    );
    
    Optional<PendingAddition> findByPersonIdAndRequestedBy(UUID personId, UUID requestedBy);
}
