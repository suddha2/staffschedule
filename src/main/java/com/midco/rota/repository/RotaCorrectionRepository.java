package com.midco.rota.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.RotaCorrection;
import com.midco.rota.util.ShiftType;

@Repository
public interface RotaCorrectionRepository extends JpaRepository<RotaCorrection, Long> {
    
    // Find corrections after a specific date
    List<RotaCorrection> findByCorrectionDateAfter(LocalDateTime dateTime);
    
    // Find corrections in a date range
    List<RotaCorrection> findByCorrectionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find all corrections for a specific employee (as corrected employee)
    List<RotaCorrection> findByCorrectedEmployeeId(Integer employeeId);
    
    // Find all corrections where employee was originally assigned
    List<RotaCorrection> findByOriginalEmployeeId(Integer employeeId);
    
    // Find corrections by location
    List<RotaCorrection> findByLocation(String location);
    
    // Find corrections by shift type
    List<RotaCorrection> findByShiftType(ShiftType shiftType);
    
    // Find corrections by day of week
    List<RotaCorrection> findByDayOfWeek(DayOfWeek dayOfWeek);
    
    // Find corrections for specific shift date range
    List<RotaCorrection> findByShiftDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Find by week identifier
    List<RotaCorrection> findByWeek(String week);
    
    // Count total corrections after a date
    Long countByCorrectionDateAfter(LocalDateTime dateTime);
    
    // Count corrections for specific employee
    Long countByCorrectedEmployeeId(Integer employeeId);
    
    // Find unassigned shifts that were manually filled (original employee was null)
    @Query("SELECT rc FROM RotaCorrection rc WHERE rc.originalEmployee IS NULL")
    List<RotaCorrection> findUnassignedCorrections();
    
    // Find unassigned corrections after a specific date
    @Query("SELECT rc FROM RotaCorrection rc WHERE rc.originalEmployee IS NULL AND rc.correctionDate > :dateTime")
    List<RotaCorrection> findUnassignedCorrectionsAfter(@Param("dateTime") LocalDateTime dateTime);
    
    // Find reassignments (original employee was changed to different employee)
    @Query("SELECT rc FROM RotaCorrection rc WHERE rc.originalEmployee IS NOT NULL AND rc.correctedEmployee IS NOT NULL")
    List<RotaCorrection> findReassignments();
    
    // Find corrections for specific employee at specific location
    @Query("SELECT rc FROM RotaCorrection rc WHERE rc.correctedEmployee.id = :employeeId AND rc.location = :location")
    List<RotaCorrection> findByEmployeeAndLocation(@Param("employeeId") Integer employeeId, 
                                                   @Param("location") String location);
    
    // Count corrections by employee and location
    @Query("SELECT COUNT(rc) FROM RotaCorrection rc WHERE rc.correctedEmployee.id = :employeeId AND rc.location = :location")
    Long countByEmployeeAndLocation(@Param("employeeId") Integer employeeId, 
                                   @Param("location") String location);
    
    // Group corrections by location with counts
    @Query("SELECT rc.location, COUNT(rc) FROM RotaCorrection rc WHERE rc.correctionDate > :dateTime " +
           "GROUP BY rc.location ORDER BY COUNT(rc) DESC")
    List<Object[]> countCorrectionsByLocation(@Param("dateTime") LocalDateTime dateTime);
    
    // Group corrections by employee with counts
    @Query("SELECT rc.correctedEmployee.id, rc.correctedEmployee.firstName, rc.correctedEmployee.lastName, COUNT(rc) " +
           "FROM RotaCorrection rc WHERE rc.correctionDate > :dateTime " +
           "GROUP BY rc.correctedEmployee.id, rc.correctedEmployee.firstName, rc.correctedEmployee.lastName " +
           "ORDER BY COUNT(rc) DESC")
    List<Object[]> countCorrectionsByEmployee(@Param("dateTime") LocalDateTime dateTime);
    
    // Find corrections for specific employee and shift type
    @Query("SELECT rc FROM RotaCorrection rc WHERE rc.correctedEmployee.id = :employeeId AND rc.shiftType = :shiftType")
    List<RotaCorrection> findByEmployeeAndShiftType(@Param("employeeId") Integer employeeId, 
                                                    @Param("shiftType") ShiftType shiftType);
    
    // Find corrections for specific employee and day of week
    @Query("SELECT rc FROM RotaCorrection rc WHERE rc.correctedEmployee.id = :employeeId AND rc.dayOfWeek = :dayOfWeek")
    List<RotaCorrection> findByEmployeeAndDay(@Param("employeeId") Integer employeeId, 
                                              @Param("dayOfWeek") DayOfWeek dayOfWeek);
    
    // Delete corrections older than a date (for cleanup)
    void deleteByCorrectionDateBefore(LocalDateTime dateTime);
}