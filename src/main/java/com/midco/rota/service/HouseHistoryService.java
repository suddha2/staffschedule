package com.midco.rota.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.opt.SolverTuning;

/**
 * Loads carers' People Planner house history (carer_house_history, V033) onto the solve's
 * employees and templates before solving.
 *
 * <p>Familiarity with a house, 0-100: the share of the carer's People Planner shifts worked
 * there, scaled so houseHistoryFullSharePct (e.g. 20%) or more is 100 - one of their own
 * houses. Fewer than houseHistoryMinShifts shifts there is 0. So a carer who mostly works
 * Riverdale is 100 there, while one who covered it for two days in June is close to 0.
 *
 * <p>A template's house is tracked when somebody has familiarity there; houses People
 * Planner can't see are never judged. With the table empty or missing nothing is tracked,
 * so regulars-first and history-backed carry-over are off and everything works as before.
 */
@Service
public class HouseHistoryService {

	private static final Logger logger = LoggerFactory.getLogger(HouseHistoryService.class);

	/** Familiarity a carry-over needs: the house must be a real part of the carer's work. */
	static final int CARRY_OVER_MIN_FAMILIARITY = 50;

	private final JdbcTemplate jdbc;

	public HouseHistoryService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void apply(List<Employee> employees, List<ShiftTemplate> templates) {
		SolverTuning tuning = SolverTuning.current();
		int minShifts = Math.max(1, tuning.getHouseHistoryMinShifts());
		int fullShare = Math.max(1, tuning.getHouseHistoryFullSharePct());
		List<Map<String, Object>> rows;
		try {
			rows = jdbc.queryForList(
					"SELECT employee_id, upper(trim(location)) AS house, share_pct FROM carer_house_history WHERE shifts >= ?",
					minShifts);
		} catch (Exception e) {
			logger.warn("House history unavailable ({}); regulars-first and history-backed carry-over are off", e.toString());
			return;
		}
		Set<String> tracked = new HashSet<>();
		Map<Integer, Map<String, Integer>> byEmployee = new HashMap<>();
		for (Map<String, Object> row : rows) {
			String house = (String) row.get("house");
			int familiarity = familiarity(((Number) row.get("share_pct")).doubleValue(), fullShare);
			if (familiarity <= 0) {
				continue;
			}
			tracked.add(house);
			byEmployee.computeIfAbsent(((Number) row.get("employee_id")).intValue(), k -> new HashMap<>()).put(house, familiarity);
		}
		int withHistory = 0;
		for (Employee e : employees) {
			Map<String, Integer> f = byEmployee.getOrDefault(e.getId(), Map.of());
			e.setHouseFamiliarity(f);
			if (!f.isEmpty()) {
				withHistory++;
			}
		}
		int trackedTemplates = 0;
		for (ShiftTemplate t : templates) {
			boolean on = t.getLocation() != null && tracked.contains(t.getLocation().trim().toUpperCase());
			t.setHistoryTracked(on);
			if (on) {
				trackedTemplates++;
			}
		}
		logger.info("House history: {} of {} carers have People Planner history (>= {} shifts, own house at {}% share); {} of {} templates at tracked houses",
				withHistory, employees.size(), minShifts, fullShare, trackedTemplates, templates.size());
	}

	/** Share of the carer's shifts at the house, scaled so fullSharePct or more is 100. */
	static int familiarity(double sharePct, int fullSharePct) {
		return (int) Math.min(100, Math.round(sharePct * 100.0 / fullSharePct));
	}

	/**
	 * "Carer new to this house" units for this carer on this template: 100 - familiarity at a
	 * tracked house (100 = stranger, 0 = one of their own houses), 0 at an untracked house.
	 */
	public static int newcomerPenalty(Employee carer, ShiftTemplate template) {
		if (carer == null || template == null || !template.isHistoryTracked()) {
			return 0;
		}
		return 100 - carer.familiarityAt(template.getLocation());
	}

	/** People Planner backs carrying this carer forward here (or the house isn't tracked). */
	public static boolean backsCarryOver(Employee carer, ShiftTemplate template) {
		return template == null || !template.isHistoryTracked()
				|| carer.familiarityAt(template.getLocation()) >= CARRY_OVER_MIN_FAMILIARITY;
	}
}
