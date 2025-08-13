package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.Shift;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Integer>  {
	
	@Query(name="Shift.findAllRegion")
	List<String>  findAllRegion();
	
	List<Shift> findAllByRegion(String region);
	
}
