package com.racines_app_back.www.domain.repository;

import com.racines_app_back.www.domain.entity.SyncQueue;
import com.racines_app_back.www.domain.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueue, UUID> {
    List<SyncQueue> findByUserIdAndStatus(UUID userId, SyncStatus status);
    List<SyncQueue> findByStatus(SyncStatus status);
    
    @Query("SELECT sq FROM SyncQueue sq WHERE sq.userId = :userId AND sq.status = 'PENDING' ORDER BY sq.createdAt ASC")
    List<SyncQueue> findPendingByUserIdOrderByCreatedAt(@Param("userId") UUID userId);
    
    @Query("SELECT sq FROM SyncQueue sq WHERE sq.status = 'FAILED' AND sq.retryCount < :maxRetries")
    List<SyncQueue> findFailedWithRetryLimit(@Param("maxRetries") int maxRetries);
    
    @Query("SELECT COUNT(sq) FROM SyncQueue sq WHERE sq.userId = :userId AND sq.status = 'PENDING'")
    long countPendingByUserId(@Param("userId") UUID userId);
}
