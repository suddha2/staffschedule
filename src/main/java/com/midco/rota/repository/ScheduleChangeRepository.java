package com.midco.rota.repository;

import com.midco.rota.model.ScheduleChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for schedule_change table
 */
@Repository
public interface ScheduleChangeRepository extends JpaRepository<ScheduleChange, Long> {
    
    /**
     * Find all changes that resulted in a specific version
     */
    List<ScheduleChange> findByToVersionId(Long toVersionId);
    
    /**
     * Find all changes from a specific version
     */
    List<ScheduleChange> findByFromVersionId(Long fromVersionId);
    
    /**
     * Find changes between two specific versions
     */
    @Query("SELECT sc FROM ScheduleChange sc " +
           "WHERE sc.fromVersionId = :fromVersionId AND sc.toVersionId = :toVersionId")
    List<ScheduleChange> findByVersionPair(@Param("fromVersionId") Long fromVersionId, 
                                           @Param("toVersionId") Long toVersionId);
    
    /**
     * Find all changes for a specific shift
     */
    List<ScheduleChange> findByShiftIdOrderByChangedAtDesc(Long shiftId);
    
    /**
     * Find changes by type for a version
     */
    @Query("SELECT sc FROM ScheduleChange sc " +
           "WHERE sc.toVersionId = :versionId AND sc.changeType = :changeType")
    List<ScheduleChange> findByVersionIdAndChangeType(@Param("versionId") Long versionId, 
                                                       @Param("changeType") ScheduleChange.ChangeType changeType);
    
    /**
     * Count changes by type for a version
     */
    @Query("SELECT COUNT(sc) FROM ScheduleChange sc " +
           "WHERE sc.toVersionId = :versionId AND sc.changeType = :changeType")
    Long countByVersionIdAndChangeType(@Param("versionId") Long versionId, 
                                       @Param("changeType") ScheduleChange.ChangeType changeType);
    
    /**
     * Find changes made by a specific user
     */
    List<ScheduleChange> findByChangedByOrderByChangedAtDesc(String changedBy);
}