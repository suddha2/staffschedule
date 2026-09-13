package com.midco.rota.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.EmployeeAvailability;
import com.midco.rota.util.AvailabilitySource;

/** Booked leave / unavailability spans. Loaded into a solve for the window and employees in scope. */
@Repository
public interface EmployeeAvailabilityRepository extends JpaRepository<EmployeeAvailability, Long> {

	/**
	 * All availability spans for the given employees that overlap the solve window
	 * [windowStart, windowEnd]. Overlap = span.start <= windowEnd AND span.end >= windowStart.
	 */
	@Query("SELECT a FROM EmployeeAvailability a "
			+ "WHERE a.employeeId IN :employeeIds "
			+ "AND a.startDate <= :windowEnd AND a.endDate >= :windowStart")
	List<EmployeeAvailability> findOverlapping(@Param("employeeIds") List<Integer> employeeIds,
			@Param("windowStart") LocalDate windowStart,
			@Param("windowEnd") LocalDate windowEnd);

	/** Used by the daily sync to upsert: find an existing row by its source system key. */
	EmployeeAvailability findBySourceAndExternalRef(AvailabilitySource source, String externalRef);
}
