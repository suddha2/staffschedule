package com.midco.rota.repository;

import com.midco.rota.model.ScheduleVersionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for schedule_version_audit table
 */
@Repository
public interface ScheduleVersionAuditRepository extends JpaRepository<ScheduleVersionAudit, Long> {
    
    /**
     * Find all audit logs for a specific version
     */
    List<ScheduleVersionAudit> findByVersionIdOrderByPerformedAtDesc(Long versionId);
    
    /**
     * Find audit logs by action type
     */
    List<ScheduleVersionAudit> findByActionOrderByPerformedAtDesc(ScheduleVersionAudit.AuditAction action);
    
    /**
     * Find audit logs by user
     */
    List<ScheduleVersionAudit> findByPerformedByOrderByPerformedAtDesc(String performedBy);
    
    /**
     * Find audit logs within a date range
     */
    @Query("SELECT sva FROM ScheduleVersionAudit sva " +
           "WHERE sva.performedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY sva.performedAt DESC")
    List<ScheduleVersionAudit> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find recent audit logs (last N entries)
     */
    @Query("SELECT sva FROM ScheduleVersionAudit sva ORDER BY sva.performedAt DESC")
    List<ScheduleVersionAudit> findRecent();
}