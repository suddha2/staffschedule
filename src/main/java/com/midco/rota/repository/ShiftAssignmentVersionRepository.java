package com.midco.rota.repository;

import com.midco.rota.model.ShiftAssignmentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for shift_assignment_version table
 */
@Repository
public interface ShiftAssignmentVersionRepository extends JpaRepository<ShiftAssignmentVersion, Long> {
    
    /**
     * Find all assignment snapshots for a specific version
     */
    List<ShiftAssignmentVersion> findByVersionId(Long versionId);
    
    /**
     * Find all assignments for a specific version and rota
     */
    List<ShiftAssignmentVersion> findByVersionIdAndRotaId(Long versionId, Integer rotaId);
    
    /**
     * Find assignments for a specific employee in a version
     */
    List<ShiftAssignmentVersion> findByVersionIdAndEmployeeId(Long versionId, Integer employeeId);
    
    /**
     * Count total assignments in a version
     */
    @Query("SELECT COUNT(sav) FROM ShiftAssignmentVersion sav WHERE sav.versionId = :versionId")
    Long countByVersionId(@Param("versionId") Long versionId);
    
    /**
     * Count assigned shifts (not null employee) in a version
     */
    @Query("SELECT COUNT(sav) FROM ShiftAssignmentVersion sav " +
           "WHERE sav.versionId = :versionId AND sav.employeeId IS NOT NULL")
    Long countAssignedByVersionId(@Param("versionId") Long versionId);
    
    /**
     * Count unassigned shifts (null employee) in a version
     */
    @Query("SELECT COUNT(sav) FROM ShiftAssignmentVersion sav " +
           "WHERE sav.versionId = :versionId AND sav.employeeId IS NULL")
    Long countUnassignedByVersionId(@Param("versionId") Long versionId);
    
    /**
     * Get unique employees in a version
     */
    @Query("SELECT DISTINCT sav.employeeId FROM ShiftAssignmentVersion sav " +
           "WHERE sav.versionId = :versionId AND sav.employeeId IS NOT NULL")
    List<Integer> findUniqueEmployeeIdsByVersionId(@Param("versionId") Long versionId);
    
    /**
     * Delete all assignments for a version
     */
    void deleteByVersionId(Long versionId);
}
