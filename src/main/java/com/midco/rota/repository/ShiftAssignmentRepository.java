package com.midco.rota.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.midco.rota.model.ShiftAssignment;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

	List<ShiftAssignment> findByRotaId(Long rotaId);

	void deleteByRotaId(Long rotaId	);
	 List<ShiftAssignment> findByRotaIdAndShiftId(Long rotaId, Long shiftId);

	/**
	 * Prior-period allocation for continuity seeding: (template id, shift date,
	 * employee id) from the most recent CURRENT-published rota for the region
	 * whose solve started before this window, restricted to the prior window
	 * [priorStart, priorEnd]. Returns {@code Object[]{templateId, date, employeeId}}.
	 * Empty when there is no prior published rota (e.g. a region's first period).
	 */
	@Query(value = """
			SELECT s.shift_template_id, s.shift_start, rsa.employee_id
			FROM rota_shift_assignment rsa
			JOIN shift s ON s.id = rsa.shift_id
			WHERE rsa.employee_id IS NOT NULL
			  AND s.shift_start BETWEEN :priorStart AND :priorEnd
			  AND rsa.rota_id = (
			      SELECT sv.rota_id FROM schedule_version sv
			      JOIN deferred_solve_request dsr ON dsr.rota_id = sv.rota_id
			      WHERE sv.is_current = true AND dsr.region = :region
			        AND dsr.start_date < :windowStart
			      ORDER BY dsr.start_date DESC
			      LIMIT 1)
			""", nativeQuery = true)
	List<Object[]> findPriorPublishedAllocation(@Param("region") String region,
			@Param("windowStart") LocalDate windowStart,
			@Param("priorStart") LocalDate priorStart,
			@Param("priorEnd") LocalDate priorEnd);
}