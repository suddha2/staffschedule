package com.midco.rota.repository;

import com.midco.rota.model.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {
    
    /**
     * Find the period that contains a specific date
     */
//    @Query("SELECT p FROM Period p WHERE :date >= p.startDate AND :date <= p.endDate")
//    Optional<Period> findByDate(@Param("date") LocalDate date);

    @Query("SELECT p FROM Period p WHERE :date BETWEEN p.startDate AND p.endDate")
    Optional<Period>  findByDate(@Param("date") LocalDate date);
    
    /**
     * Find all active periods
     */
    @Query("SELECT p FROM Period p WHERE p.isActive = true ORDER BY p.startDate")
    java.util.List<Period> findAllActive();
    
    @Query("SELECT MIN(p.startDate) FROM Period  p WHERE p.isActive = true")
    Optional<LocalDate> findEarliestStartDate();
    
    @Query("SELECT MAX(p.endDate) FROM Period p WHERE p.isActive = true")
    Optional<LocalDate> findLatestEndDate();
    
}