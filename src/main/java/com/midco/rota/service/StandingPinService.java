package com.midco.rota.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.dto.StandingPin;

/**
 * Lists standing pins by region and removes them, one or many at a time. Each removal keeps the
 * pins it took out, so the region's latest removal can be undone. Solves read pins when they
 * start, so a removal takes effect on the next solve. Rule breaks throw
 * IllegalArgumentException with a message for the user.
 */
@Service
public class StandingPinService {

	private final JdbcTemplate jdbc;
	private final NamedParameterJdbcTemplate named;

	public StandingPinService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
		this.named = new NamedParameterJdbcTemplate(jdbc);
	}

	public List<StandingPin> list(String region) {
		return jdbc.query("select p.id, p.employee_id, e.first_name, e.last_name, e.active, btrim(t.location) loc, t.shift_type, "
				+ "t.day_of_week, p.week_of_period from pinned_template_assignment p "
				+ "join shift_templates t on t.id = p.shift_template_id left join employee e on e.id = p.employee_id "
				+ "where t.region = ? order by e.first_name, e.last_name, loc, t.shift_type, p.week_of_period",
				(rs, i) -> {
					String first = rs.getString("first_name");
					String carer = first == null ? "Carer " + rs.getLong("employee_id")
							: (first + " " + (rs.getString("last_name") == null ? "" : rs.getString("last_name"))).trim();
					return new StandingPin(rs.getLong("id"), rs.getLong("employee_id"), carer, rs.getBoolean("active"),
							rs.getString("loc"), rs.getString("shift_type"), rs.getString("day_of_week"), rs.getInt("week_of_period"));
				}, region);
	}

	/** Removes these pins, which must all be in the region. Returns the removal's id. */
	@Transactional
	public long remove(String region, List<Long> pinIds, String user) {
		if (region == null || region.isBlank() || pinIds == null || pinIds.isEmpty()) {
			throw new IllegalArgumentException("Choose the pins to remove");
		}
		Map<String, Object> params = Map.of("ids", pinIds, "region", region);
		List<Map<String, Object>> pins = named.queryForList("select p.id, p.shift_template_id, p.employee_id, p.week_of_period, "
				+ "p.pinned_at, p.pinned_by_user_id, e.first_name, e.last_name from pinned_template_assignment p "
				+ "join shift_templates t on t.id = p.shift_template_id left join employee e on e.id = p.employee_id "
				+ "where p.id in (:ids) and t.region = :region", params);
		if (pins.size() != pinIds.size()) {
			throw new IllegalArgumentException("Some of these pins have changed. Refresh and try again");
		}
		String carers = pins.stream()
				.map(p -> (p.get("first_name") + " " + (p.get("last_name") == null ? "" : p.get("last_name"))).trim())
				.distinct().sorted().collect(Collectors.joining(", "));
		Long id = jdbc.queryForObject("insert into standing_pin_removal (region, removed_by, pin_count, carers) values (?, ?, ?, ?) returning id",
				Long.class, region, user, pins.size(), carers);
		for (Map<String, Object> p : pins) {
			jdbc.update("insert into standing_pin_removal_item (removal_id, shift_template_id, employee_id, week_of_period, pinned_at, pinned_by_user_id) "
					+ "values (?, ?, ?, ?, ?, ?)", id, p.get("shift_template_id"), p.get("employee_id"), p.get("week_of_period"),
					p.get("pinned_at"), p.get("pinned_by_user_id"));
		}
		named.update("delete from pinned_template_assignment where id in (:ids)", params);
		return id;
	}

	/** Removes the standing pin behind a "Pins not applied" row, if it still exists. */
	@Transactional
	public boolean removeSkipped(long skippedPinId, String user) {
		List<Map<String, Object>> rows = jdbc.queryForList("select p.id, t.region from rota_skipped_pin s "
				+ "join pinned_template_assignment p on p.shift_template_id = s.shift_template_id "
				+ "and p.week_of_period = s.week_of_period and p.employee_id = s.employee_id "
				+ "join shift_templates t on t.id = p.shift_template_id where s.id = ?", skippedPinId);
		if (rows.isEmpty()) {
			return false;
		}
		remove((String) rows.get(0).get("region"), List.of(((Number) rows.get(0).get("id")).longValue()), user);
		return true;
	}

	/** Puts back the region's latest removal. A pin that has been set again since is left as it is. */
	@Transactional
	public int undo(long id, String user) {
		List<Map<String, Object>> runs = jdbc.queryForList("select region, undone_at from standing_pin_removal where id = ?", id);
		if (runs.isEmpty()) {
			throw new IllegalArgumentException("Removal not found");
		}
		if (runs.get(0).get("undone_at") != null) {
			throw new IllegalArgumentException("This removal has already been undone");
		}
		Long latest = jdbc.queryForObject("select max(id) from standing_pin_removal where region = ? and undone_at is null",
				Long.class, runs.get(0).get("region"));
		if (latest == null || latest != id) {
			throw new IllegalArgumentException("Undo the later removals first");
		}
		int restored = jdbc.update("insert into pinned_template_assignment (shift_template_id, employee_id, week_of_period, pinned_at, pinned_by_user_id) "
				+ "select i.shift_template_id, i.employee_id, i.week_of_period, i.pinned_at, i.pinned_by_user_id "
				+ "from standing_pin_removal_item i "
				+ "where i.removal_id = ? and exists (select 1 from shift_templates t where t.id = i.shift_template_id) "
				+ "and exists (select 1 from employee e where e.id = i.employee_id) "
				+ "on conflict (shift_template_id, week_of_period, employee_id) do nothing", id);
		jdbc.update("update standing_pin_removal set undone_by = ?, undone_at = now() where id = ?", user, id);
		return restored;
	}

	public List<Map<String, Object>> history(String region) {
		return jdbc.queryForList("select id, removed_by, removed_at, pin_count, carers, undone_by, undone_at "
				+ "from standing_pin_removal where region = ? order by id desc limit 50", region);
	}

	/** What ticking "Pin all" on this rota would do, so the save dialog can warn about it. */
	public Map<String, Object> pinAllImpact(long rotaId) {
		List<String> regions = jdbc.queryForList("select distinct t.region from rota_shift_assignment r join shift s on s.id = r.shift_id "
				+ "join shift_templates t on t.id = s.shift_template_id where r.rota_id = ?", String.class, rotaId);
		Integer wouldPin = jdbc.queryForObject("select count(*) from rota_shift_assignment where rota_id = ? and employee_id is not null",
				Integer.class, rotaId);
		if (regions.isEmpty()) {
			return Map.of("region", "", "currentPins", 0, "wouldPin", 0);
		}
		Integer current = jdbc.queryForObject("select count(*) from pinned_template_assignment p join shift_templates t on t.id = p.shift_template_id "
				+ "where t.region = ?", Integer.class, regions.get(0));
		return Map.of("region", regions.get(0), "currentPins", current == null ? 0 : current, "wouldPin", wouldPin == null ? 0 : wouldPin);
	}
}
