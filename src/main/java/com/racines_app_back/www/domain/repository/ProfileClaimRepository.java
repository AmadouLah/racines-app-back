package com.racines_app_back.www.domain.repository;

import com.racines_app_back.www.domain.entity.ProfileClaim;
import com.racines_app_back.www.domain.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileClaimRepository extends JpaRepository<ProfileClaim, UUID> {
    List<ProfileClaim> findByStatus(ClaimStatus status);
    List<ProfileClaim> findByUserId(UUID userId);
    List<ProfileClaim> findByPersonId(UUID personId);
    
    @Query("SELECT pc FROM ProfileClaim pc WHERE pc.personId = :personId AND pc.status = 'PENDING'")
    List<ProfileClaim> findPendingClaimsByPersonId(@Param("personId") UUID personId);
    
    Optional<ProfileClaim> findByPersonIdAndUserId(UUID personId, UUID userId);
    
    @Query("SELECT pc FROM ProfileClaim pc WHERE pc.userId = :userId AND pc.status = :status")
    List<ProfileClaim> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") ClaimStatus status);
}
