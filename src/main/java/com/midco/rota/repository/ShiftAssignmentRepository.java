package com.midco.rota.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.midco.rota.model.ShiftAssignment;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

	List<ShiftAssignment> findByRotaId(Long rotaId);

	/**
	 * One-row comparison stats for a rota, for the Spread-vs-Continuity compare view.
	 * Returns {@code [slots, filled, carers, distinct_patterns, gender_mismatch,
	 * restricted_day, restricted_shift, restricted_service, below_min_hours]}.
	 * distinct_patterns is distinct (employee, weekday, location, shift-type) among
	 * filled slots — filled/distinct_patterns is the weeks-per-pattern stability metric.
	 * The violation counts use the (now-soft) gender/restriction rules; below_min_hours
	 * counts carers whose period hours fall under 4× their weekly minimum.
	 */
	@Query(value = """
			SELECT
			  count(*) AS slots,
			  count(rsa.employee_id) AS filled,
			  count(DISTINCT rsa.employee_id) AS carers,
			  count(DISTINCT (rsa.employee_id, extract(isodow FROM s.shift_start), st.location, st.shift_type))
			     FILTER (WHERE rsa.employee_id IS NOT NULL) AS distinct_patterns,
			  count(*) FILTER (WHERE rsa.employee_id IS NOT NULL
			     AND upper(coalesce(st.required_gender,'')) IN ('MALE','FEMALE')
			     AND upper(st.required_gender) <> upper(coalesce(e.gender,''))) AS gender_mismatch,
			  count(*) FILTER (WHERE rsa.employee_id IS NOT NULL AND coalesce(e.restricted_days,'')<>''
			     AND upper(to_char(s.shift_start,'FMDay')) = ANY(string_to_array(upper(replace(e.restricted_days,' ','')),','))) AS restricted_day,
			  count(*) FILTER (WHERE rsa.employee_id IS NOT NULL AND coalesce(e.restricted_shifts,'')<>''
			     AND st.shift_type = ANY(string_to_array(upper(replace(e.restricted_shifts,' ','')),','))) AS restricted_shift,
			  count(*) FILTER (WHERE rsa.employee_id IS NOT NULL AND coalesce(e.restricted_service,'')<>''
			     AND upper(st.location) = ANY(string_to_array(upper(replace(e.restricted_service,', ',',')),','))) AS restricted_service,
			  ( SELECT count(*) FROM (
			      SELECT rsa2.employee_id, sum(st2.total_hours) h, max(e2.min_hours) mh
			      FROM rota_shift_assignment rsa2
			      JOIN shift s2 ON s2.id=rsa2.shift_id
			      JOIN shift_templates st2 ON st2.id=s2.shift_template_id
			      JOIN employee e2 ON e2.id=rsa2.employee_id
			      WHERE rsa2.rota_id=:rid AND rsa2.employee_id IS NOT NULL
			      GROUP BY rsa2.employee_id
			      HAVING sum(coalesce(st2.total_hours,0)) < coalesce(max(e2.min_hours),0) * 4
			  ) z ) AS below_min_hours
			FROM rota_shift_assignment rsa
			JOIN shift s ON s.id=rsa.shift_id
			JOIN shift_templates st ON st.id=s.shift_template_id
			LEFT JOIN employee e ON e.id=rsa.employee_id
			WHERE rsa.rota_id=:rid
			""", nativeQuery = true)
	List<Object[]> compareStats(@Param("rid") Long rotaId);

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