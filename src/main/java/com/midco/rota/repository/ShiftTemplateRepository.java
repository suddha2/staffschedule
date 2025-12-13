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
}
