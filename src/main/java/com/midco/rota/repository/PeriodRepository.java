package com.midco.rota.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.Period;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {

	/**
	 * Find the period that contains a specific date
	 */
//    @Query("SELECT p FROM Period p WHERE :date >= p.startDate AND :date <= p.endDate")
//    Optional<Period> findByDate(@Param("date") LocalDate date);

	@Query("SELECT p FROM Period p WHERE :date BETWEEN p.startDate AND p.endDate")
	Optional<Period> findByDate(@Param("date") LocalDate date);

	/**
	 * Find all active periods
	 */
	@Query("SELECT p FROM Period p WHERE p.isActive = true ORDER BY p.startDate")
	java.util.List<Period> findAllActive();

	@Query("SELECT MIN(p.startDate) FROM Period  p WHERE p.isActive = true")
	Optional<LocalDate> findEarliestStartDate();

	@Query("SELECT MAX(p.endDate) FROM Period p WHERE p.isActive = true")
	Optional<LocalDate> findLatestEndDate();

	@Query("SELECT COUNT(p) > 0 FROM Period p " + "WHERE (YEAR(p.startDate) = :year OR YEAR(p.endDate) = :year) "
			+ "AND p.isActive = true")
	boolean existsForYear(@Param("year") int year);

	@Query("SELECT p FROM Period p WHERE p.isActive = false ORDER BY p.startDate")
	List<Period> findAllArchived();

	/**
	 * Find archived periods by year range
	 */
	@Query("SELECT p FROM Period p WHERE p.isActive = false "
			+ "AND ((YEAR(p.startDate) = :startYear AND YEAR(p.endDate) = :endYear) "
			+ "OR (YEAR(p.startDate) = :startYear AND YEAR(p.endDate) = :startYear) "
			+ "OR (YEAR(p.endDate) = :endYear AND YEAR(p.startDate) = :endYear)) " + "ORDER BY p.startDate")
	List<Period> findArchivedByYearRange(@Param("startYear") int startYear, @Param("endYear") int endYear);

}