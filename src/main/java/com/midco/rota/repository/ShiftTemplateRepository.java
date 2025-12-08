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
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Integer>  {
	
	@Query(name="ShiftTemplate.findAllRegion")
	List<String>  findAllRegion();
	
	@Query("SELECT s FROM ShiftTemplate s WHERE s.region = :region and s.totalHours > 0 and active=true")
	List<ShiftTemplate> findAllByRegion(@Param("region") String region);

	ShiftTemplate findByLocationAndShiftTypeAndStartTimeAndDayOfWeek(
		    String location, 
		    ShiftType shiftType, 
		    LocalTime startTime,
		    DayOfWeek dayOfWeek
		);
}
