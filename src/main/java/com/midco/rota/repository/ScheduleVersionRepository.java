package com.midco.rota.repository;

import com.midco.rota.model.ScheduleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for schedule_version table
 */
@Repository
public interface ScheduleVersionRepository extends JpaRepository<ScheduleVersion, Long> {
    
    /**
     * Find all versions for a specific rota, ordered by version number descending
     */
    List<ScheduleVersion> findByRotaIdOrderByVersionNumberDesc(Long rotaId);
    
    /**
     * Find a specific version by rota ID and version number
     */
    Optional<ScheduleVersion> findByRotaIdAndVersionNumber(Long rotaId, Long versionNumber);
    
    /**
     * Find the current version for a rota
     */
    @Query("SELECT sv FROM ScheduleVersion sv WHERE sv.rotaId = :rotaId AND sv.isCurrent = true")
    Optional<ScheduleVersion> findCurrentVersionByRotaId(@Param("rotaId") Long rotaId);
    
    /**
     * Get the maximum version number for a rota
     */
    @Query("SELECT MAX(sv.versionNumber) FROM ScheduleVersion sv WHERE sv.rotaId = :rotaId")
    Optional<Integer> findMaxVersionNumber(@Param("rotaId") Long rotaId);
    
    /**
     * Count total versions for a rota
     */
    @Query("SELECT COUNT(sv) FROM ScheduleVersion sv WHERE sv.rotaId = :rotaId")
    Integer countByRotaId(@Param("rotaId") Long rotaId);
    
    /**
     * Count how many versions are marked as current (should always be 0 or 1)
     * Used for validation
     */
    @Query("SELECT COUNT(sv) FROM ScheduleVersion sv WHERE sv.rotaId = :rotaId AND sv.isCurrent = true")
    Integer countCurrentVersions(@Param("rotaId") Long rotaId);
    
    /**
     * Find recent versions for a rota
     */
    @Query("SELECT sv FROM ScheduleVersion sv WHERE sv.rotaId = :rotaId ORDER BY sv.createdAt DESC")
    List<ScheduleVersion> findRecentByRotaId(@Param("rotaId") Long rotaId);
    
    /**
     * Find versions created by a specific user
     */
    List<ScheduleVersion> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
