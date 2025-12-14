package com.midco.rota.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftTemplate;

public interface ShiftRepository extends JpaRepository<Shift, Long>  {

//	Shift findByShiftTemplateAndStartTime(ShiftTemplate template, LocalDate date);
	
	@Query("SELECT s FROM Shift s WHERE s.shiftTemplate = :template AND s.shiftStart = :date")
	Shift findByShiftTemplateAndStartTime(@Param("template") ShiftTemplate template,
	                                      @Param("date") LocalDate date);


}
