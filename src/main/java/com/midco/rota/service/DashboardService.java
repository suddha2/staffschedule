package com.midco.rota.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.midco.rota.dto.DashboardSummary;
import com.midco.rota.dto.DashboardSummary.Period;
import com.midco.rota.dto.DashboardSummary.RegionRow;
import com.midco.rota.dto.DashboardSummary.RotaState;
import com.midco.rota.repository.RegionRepository;

/**
 * The home screen's figures. A region's rota for a period is its latest solve request for those
 * dates that produced a rota; a request still in the queue counts as solving. Hours below contract
 * and pins not applied are for the next period's rota, as that is the one still being worked on.
 */
@Service
public class DashboardService {

	private final JdbcTemplate jdbc;
	private final RegionRepository regions;
	private final ServiceBulkUpdateService services;

	public DashboardService(JdbcTemplate jdbc, RegionRepository regions, ServiceBulkUpdateService services) {
		this.jdbc = jdbc;
		this.regions = regions;
		this.services = services;
	}

	public DashboardSummary summary() {
		LocalDate today = LocalDate.now();
		Period current = period("start_date <= ? and end_date >= ? order by start_date desc", today, today);
		Period next = period("start_date > ? order by start_date", today);

		List<RegionRow> rows = new ArrayList<>();
		int namesToFix = 0;
		for (String region : regions.findOperatingRegions()) {
			Integer carers = jdbc.queryForObject("select count(*) from employee where active and preferred_region = ?", Integer.class, region);
			RotaState now = rotaState(region, current);
			RotaState upcoming = rotaState(region, next);
			Long rotaId = upcoming.rotaId();
			rows.add(new RegionRow(region, carers == null ? 0 : carers, now, upcoming,
					rotaId == null ? 0 : belowContract(region, rotaId, next), rotaId == null ? 0 : pinsNotApplied(rotaId)));
			namesToFix += services.namesToFix(region).size();
		}
		return new DashboardSummary(today, current, next, rows, count("select count(*) from shift_request where status = 'PENDING'"),
				namesToFix, count("select count(*) from deferred_solve_request where not completed"));
	}

	private Period period(String where, Object... args) {
		List<Period> found = jdbc.query("select name, start_date, end_date from pay_cycle_periods where active and " + where + " limit 1",
				(rs, i) -> new Period(rs.getString("name"), rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate()), args);
		return found.isEmpty() ? null : found.get(0);
	}

	private RotaState rotaState(String region, Period period) {
		if (period == null) {
			return new RotaState("NOT_STARTED", null, 0, 0, 0);
		}
		List<Map<String, Object>> requests = jdbc.queryForList("select rota_id, completed from deferred_solve_request "
				+ "where region = ? and start_date = ? and end_date = ? and (rota_id is not null or not completed) order by id desc",
				region, period.startDate(), period.endDate());
		Long rotaId = requests.stream().map(r -> (Number) r.get("rota_id")).filter(java.util.Objects::nonNull)
				.map(Number::longValue).findFirst().orElse(null);
		boolean solving = requests.stream().anyMatch(r -> Boolean.FALSE.equals(r.get("completed")));
		return state(solving, rotaId, rotaId == null ? List.of() : jdbc.queryForList(
				"select count(*) shifts, count(employee_id) filled from rota_shift_assignment where rota_id = ?", rotaId));
	}

	private static RotaState state(boolean solving, Long rotaId, List<Map<String, Object>> counts) {
		int shifts = counts.isEmpty() ? 0 : ((Number) counts.get(0).get("shifts")).intValue();
		int filled = counts.isEmpty() ? 0 : ((Number) counts.get(0).get("filled")).intValue();
		String status = solving ? "SOLVING" : rotaId == null ? "NOT_STARTED" : "READY";
		return new RotaState(status, rotaId, shifts, filled, shifts - filled);
	}

	/** Permanent carers under their contracted hours in at least one week; sleep-ins don't count. */
	private int belowContract(String region, long rotaId, Period period) {
		Integer n = jdbc.queryForObject("""
				with weeks as (select generate_series(?::date, ?::date - 6, interval '7 days')::date w),
				hours as (select r.employee_id, date_trunc('week', s.shift_start)::date w, sum(t.total_hours) h
				            from rota_shift_assignment r join shift s on s.id = r.shift_id
				            join shift_templates t on t.id = s.shift_template_id
				           where r.rota_id = ? and r.employee_id is not null and t.shift_type <> 'SLEEP_IN' group by 1, 2)
				select count(distinct e.id) from employee e cross join weeks left join hours h on h.employee_id = e.id and h.w = weeks.w
				 where e.active and e.preferred_region = ? and e.contract_type = 'PERMANENT' and coalesce(e.min_hours, 0) > 0
				   and coalesce(h.h, 0) < e.min_hours
				""", Integer.class, period.startDate(), period.endDate(), rotaId, region);
		return n == null ? 0 : n;
	}

	private int pinsNotApplied(long rotaId) {
		Integer n = jdbc.queryForObject("select count(*) from rota_skipped_pin where rota_id = ?", Integer.class, rotaId);
		return n == null ? 0 : n;
	}

	private int count(String sql) {
		Integer n = jdbc.queryForObject(sql, Integer.class);
		return n == null ? 0 : n;
	}
}
