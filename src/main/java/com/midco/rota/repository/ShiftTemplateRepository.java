package com.midco.rota.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.util.ShiftType;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Integer> {

	@Query(name = "ShiftTemplate.findAllRegion")
	List<String> findAllRegion();

	@Query("SELECT distinct s.location FROM ShiftTemplate s WHERE s.region = :region ")
	List<String> findAllServiceLocation(@Param("region") String region);

	@Query("SELECT s FROM ShiftTemplate s WHERE s.region = :region and s.totalHours > 0 and active=true")
	List<ShiftTemplate> findAllByRegion(@Param("region") String region);

	ShiftTemplate findByLocationAndShiftTypeAndStartTimeAndDayOfWeek(String location, ShiftType shiftType,
			LocalTime startTime, DayOfWeek dayOfWeek);

	/**
	 * Find all active shift templates
	 */
	List<ShiftTemplate> findByActiveTrue();

	/**
	 * Find all shift templates by region
	 */
	List<ShiftTemplate> findByRegion(String region);

	/**
	 * Find active shift templates by region
	 */
	List<ShiftTemplate> findByRegionAndActiveTrue(String region);

	/**
	 * Find all shift templates by location (service)
	 */
	List<ShiftTemplate> findByLocation(String location);

	/**
	 * Find active shift templates by location
	 */
	List<ShiftTemplate> findByLocationAndActiveTrue(String location);

	/**
	 * Find shift templates by region and location
	 */
	List<ShiftTemplate> findByRegionAndLocation(String region, String location);

	/**
	 * Find shift templates by day of week
	 */
	List<ShiftTemplate> findByDayOfWeek(DayOfWeek dayOfWeek);

	/**
	 * Find shift templates by shift type
	 */
	List<ShiftTemplate> findByShiftType(ShiftType shiftType);

	/**
	 * Find shift templates by location, day, and shift type
	 */
	List<ShiftTemplate> findByLocationAndDayOfWeekAndShiftType(String location, DayOfWeek dayOfWeek,
			ShiftType shiftType);



	/**
	 * Get all distinct locations for a region
	 */
	@Query("SELECT DISTINCT s.location FROM ShiftTemplate s WHERE s.region = :region AND s.active = true ORDER BY s.location")
	List<String> findLocationsByRegion(@Param("region") String region);

	/**
	 * Count templates by location
	 */
	long countByLocation(String location);

	/**
	 * Find templates ordered by priority
	 */
	List<ShiftTemplate> findByActiveTrueOrderByPriorityAsc();

	/**
	 * Find templates by region ordered by priority
	 */
	List<ShiftTemplate> findByRegionAndActiveTrueOrderByPriorityAsc(String region);
	
	
	List<ShiftTemplate> findByLocationAndShiftTypeAndRegion(
		    String location,
		    ShiftType shiftType,
		    String region
		);

	/**
	 * Find any ACTIVE template whose natural-key tuple
	 * (region, location, day, type, startTime, endTime, breakStart, breakEnd)
	 * matches the given values, excluding the row with {@code excludeId} (pass
	 * {@code null} when called from POST). Used by both POST and PUT to reject
	 * saves that would create a true duplicate, while still allowing the
	 * legitimate 2-carer pattern of identical times with <i>different</i> break
	 * windows.
	 *
	 * <p>Implemented as a native query so we can use PostgreSQL's
	 * {@code IS NOT DISTINCT FROM} for null-safe equality on the break times,
	 * and explicit {@code CAST}s on the nullable parameters. PostgreSQL JDBC
	 * cannot infer the type of a parameter that only appears in an {@code IS
	 * NULL} check (would fail with "could not determine data type of parameter
	 * $8"), so the casts are non-negotiable.
	 *
	 * <p>Enum parameters ({@code dayOfWeek}, {@code shiftType}) come in as
	 * strings because native queries skip the JPA enum converter — callers
	 * pass {@code DayOfWeek#name()} / {@code ShiftType#name()}.
	 */
	@Query(value = "SELECT * FROM shift_templates t " +
			"WHERE t.active = true " +
			"  AND t.region = :region " +
			"  AND t.location = :location " +
			"  AND t.day_of_week = :dayOfWeek " +
			"  AND t.shift_type = :shiftType " +
			"  AND t.start_time = :startTime " +
			"  AND t.end_time = :endTime " +
			"  AND t.break_start IS NOT DISTINCT FROM CAST(:breakStart AS time) " +
			"  AND t.break_end   IS NOT DISTINCT FROM CAST(:breakEnd   AS time) " +
			"  AND (CAST(:excludeId AS integer) IS NULL OR t.id <> CAST(:excludeId AS integer))",
			nativeQuery = true)
	List<ShiftTemplate> findActiveDuplicates(@Param("region") String region,
			@Param("location") String location,
			@Param("dayOfWeek") String dayOfWeek,
			@Param("shiftType") String shiftType,
			@Param("startTime") LocalTime startTime,
			@Param("endTime") LocalTime endTime,
			@Param("breakStart") LocalTime breakStart,
			@Param("breakEnd") LocalTime breakEnd,
			@Param("excludeId") Integer excludeId);
}
