package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.ShiftRequest;
import com.midco.rota.util.ShiftRequestStatus;

@Repository
public interface ShiftRequestRepository extends JpaRepository<ShiftRequest, Long> {

	List<ShiftRequest> findByRotaId(Long rotaId);

	List<ShiftRequest> findByRotaIdAndStatus(Long rotaId, ShiftRequestStatus status);

	List<ShiftRequest> findByStatus(ShiftRequestStatus status);

	List<ShiftRequest> findByShiftAssignmentIdAndStatus(Long shiftAssignmentId, ShiftRequestStatus status);

	List<ShiftRequest> findByEmployeeIdOrderByRequestedAtDesc(Integer employeeId);

	List<ShiftRequest> findByEmployeeIdAndStatusOrderByRequestedAtDesc(Integer employeeId, ShiftRequestStatus status);

	boolean existsByShiftAssignmentIdAndEmployeeIdAndStatus(Long shiftAssignmentId, Integer employeeId,
			ShiftRequestStatus status);

	/** Requests for a slot received since the given time (its most recent publish). */
	long countByShiftAssignmentIdAndRequestedAtAfter(Long shiftAssignmentId, java.time.LocalDateTime since);

	/** Distinct slots that have ever been requested — the working set for the
	 *  availability sweep. */
	@org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.shiftAssignment.id FROM ShiftRequest r")
	List<Long> findDistinctShiftAssignmentIds();
}
