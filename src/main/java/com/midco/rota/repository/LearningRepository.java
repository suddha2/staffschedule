package com.midco.rota.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.Learning;
import com.midco.rota.model.Learning.LearningType;

@Repository
public interface LearningRepository extends JpaRepository<Learning, Long> {
    
    // Find all pending (unapplied) learnings
    List<Learning> findByAppliedFalse();
    
    // Find all applied learnings
    List<Learning> findByAppliedTrue();
    
    // Find pending learnings with confidence above threshold
    List<Learning> findByAppliedFalseAndConfidenceGreaterThanEqual(Double minConfidence);
    
    // Find pending learnings by type
    List<Learning> findByTypeAndAppliedFalse(LearningType type);
    
    // Find all learnings for specific employee
    List<Learning> findByEmployeeId(Integer employeeId);
    
    // Find pending learnings for specific employee
    List<Learning> findByEmployeeIdAndAppliedFalse(Integer employeeId);
    
    // Find learnings by parameter (e.g., specific location name)
    List<Learning> findByParameter(String parameter);
    
    // Find learnings discovered after a date
    List<Learning> findByDiscoveredDateAfter(LocalDateTime dateTime);
    
    // Find learnings applied after a date
    List<Learning> findByAppliedDateAfter(LocalDateTime dateTime);
    
    // Find high confidence pending learnings (for auto-apply)
    @Query("SELECT l FROM Learning l WHERE l.applied = false AND l.confidence >= :threshold ORDER BY l.confidence DESC")
    List<Learning> findHighConfidencePendingLearnings(@Param("threshold") Double threshold);
    
    // Find learnings for specific employee and type
    @Query("SELECT l FROM Learning l WHERE l.employeeId = :employeeId AND l.type = :type")
    List<Learning> findByEmployeeIdAndType(@Param("employeeId") Integer employeeId, 
                                          @Param("type") LearningType type);
    
    // Check if learning already exists for employee/parameter combination
    @Query("SELECT l FROM Learning l WHERE l.employeeId = :employeeId AND l.parameter = :parameter AND l.type = :type")
    List<Learning> findDuplicateLearning(@Param("employeeId") Integer employeeId, 
                                        @Param("parameter") String parameter, 
                                        @Param("type") LearningType type);
    
    // Count pending learnings by type
    @Query("SELECT l.type, COUNT(l) FROM Learning l WHERE l.applied = false GROUP BY l.type")
    List<Object[]> countPendingByType();
    
    // Count all learnings by type
    @Query("SELECT l.type, COUNT(l) FROM Learning l GROUP BY l.type")
    List<Object[]> countByType();
    
    // Get average confidence of pending learnings
    @Query("SELECT AVG(l.confidence) FROM Learning l WHERE l.applied = false")
    Double getAveragePendingConfidence();
    
    // Get statistics
    @Query("SELECT " +
           "COUNT(l), " +
           "SUM(CASE WHEN l.applied = true THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN l.applied = false THEN 1 ELSE 0 END), " +
           "AVG(l.confidence), " +
           "AVG(l.supportingEvidence) " +
           "FROM Learning l")
    Object[] getLearningStats();
    
    // Find learnings with low confidence (might be noise)
    @Query("SELECT l FROM Learning l WHERE l.confidence < :threshold AND l.applied = false")
    List<Learning> findLowConfidenceLearnings(@Param("threshold") Double threshold);
    
    // Find learnings by type ordered by confidence
    @Query("SELECT l FROM Learning l WHERE l.type = :type ORDER BY l.confidence DESC")
    List<Learning> findByTypeOrderByConfidenceDesc(@Param("type") LearningType type);
    
    // Find pending learnings for a specific employee ordered by confidence
    @Query("SELECT l FROM Learning l WHERE l.employeeId = :employeeId AND l.applied = false ORDER BY l.confidence DESC")
    List<Learning> findPendingByEmployeeOrderedByConfidence(@Param("employeeId") Integer employeeId);
    
    // Delete old applied learnings (cleanup)
    void deleteByAppliedTrueAndAppliedDateBefore(LocalDateTime dateTime);
}