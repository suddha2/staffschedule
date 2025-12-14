package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.midco.rota.model.ShiftAssignment;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

	List<ShiftAssignment> findByRotaId(Long rotaId);

	void deleteByRotaId(Long rotaId	);
	 List<ShiftAssignment> findByRotaIdAndShiftId(Long rotaId, Long shiftId);
}