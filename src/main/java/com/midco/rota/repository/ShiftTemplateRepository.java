package com.midco.rota.repository;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.ShiftTemplate;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Integer>  {
	
	@Query(name="ShiftTemplate.findAllRegion")
	List<String>  findAllRegion();
	
	@Query("SELECT s FROM ShiftTemplate s WHERE s.region = :region and s.totalHours > 0")
	List<ShiftTemplate> findAllByRegion(@Param("region") String region);

	ShiftTemplate findByLocationAndShiftTypeAndStartTime(String location, String shiftType, LocalTime startTime);
	
}
