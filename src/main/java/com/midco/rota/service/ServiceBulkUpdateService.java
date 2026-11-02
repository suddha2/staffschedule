package com.midco.rota.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.dto.ServiceBulkPreview;
import com.midco.rota.dto.ServiceBulkRequest;
import com.midco.rota.dto.ServiceNameToFix;
import com.midco.rota.util.ServiceListEdit;

/**
 * Renames, merges or removes a service name across a region. The name is stored as text in
 * templates, carers' lists, working patterns, the publish log, house history and the service
 * master, so every run plans its row-by-row changes first: the preview shows that plan, apply
 * writes it in one transaction and records the old values, and undo writes them back. Records of
 * what happened (skipped pins, corrections, feeder rows) keep the old name. Past rotas link to
 * templates by id, so they show the new name and carry-over is unaffected.
 *
 * Refused while a solve or live session is running for the region, as those hold the old name in
 * memory. Rule breaks throw IllegalArgumentException with a message for the user.
 */
@Service
public class ServiceBulkUpdateService {

	private static final int MAX_NAME = 50; // shift_templates.location

	/** One row to change. {@code label} is the carer's name, for the preview. */
	record Change(String table, String rowId, String column, String oldValue, String newValue, String label) {
	}

	private final JdbcTemplate jdbc;
	private final LiveSolverSessionService liveSessions;

	public ServiceBulkUpdateService(JdbcTemplate jdbc, LiveSolverSessionService liveSessions) {
		this.jdbc = jdbc;
		this.liveSessions = liveSessions;
	}

	public ServiceBulkPreview preview(ServiceBulkRequest req) {
		return summarise(plan(req), warnings(req));
	}

	@Transactional
	public ServiceBulkPreview apply(ServiceBulkRequest req, String user) {
		refuseWhileSolving(req.region());
		List<Change> changes = plan(req);
		Long id = jdbc.queryForObject("insert into service_bulk_update (region, action, name, new_name, changed_by, row_count) "
				+ "values (?, ?, ?, ?, ?, ?) returning id", Long.class, req.region(), req.action(), req.name().trim(),
				"REMOVE".equals(req.action()) ? null : req.newName().trim(), user, changes.size());
		for (Change c : changes) {
			write(c.table(), c.rowId(), c.column(), c.oldValue(), c.newValue());
			jdbc.update("insert into service_bulk_update_change (update_id, table_name, row_id, column_name, old_value, new_value) "
					+ "values (?, ?, ?, ?, ?, ?)", id, c.table(), c.rowId(), c.column(), c.oldValue(), c.newValue());
		}
		return summarise(changes, List.of());
	}

	/** Undoes a run, if it is the region's latest and nothing has changed those rows since. */
	@Transactional
	public void undo(long id, String user) {
		List<Map<String, Object>> runs = jdbc.queryForList("select region, undone_at from service_bulk_update where id = ?", id);
		if (runs.isEmpty()) {
			throw new IllegalArgumentException("Update not found");
		}
		if (runs.get(0).get("undone_at") != null) {
			throw new IllegalArgumentException("This update has already been undone");
		}
		String region = (String) runs.get(0).get("region");
		refuseWhileSolving(region);
		Long latest = jdbc.queryForObject("select max(id) from service_bulk_update where region = ? and undone_at is null",
				Long.class, region);
		if (latest == null || latest != id) {
			throw new IllegalArgumentException("Undo the later updates first");
		}
		List<Map<String, Object>> changes = jdbc.queryForList(
				"select table_name, row_id, column_name, old_value, new_value from service_bulk_update_change where update_id = ?", id);
		for (Map<String, Object> c : changes) {
			String table = (String) c.get("table_name"), rowId = (String) c.get("row_id"), column = (String) c.get("column_name");
			String old = (String) c.get("old_value"), now = (String) c.get("new_value");
			if (!java.util.Objects.equals(read(table, rowId, column, now), now)) {
				throw new IllegalArgumentException("Can't undo: some of these records have changed since");
			}
			write(table, rowId, column, now, old);
		}
		jdbc.update("update service_bulk_update set undone_by = ?, undone_at = now() where id = ?", user, id);
	}

	public List<Map<String, Object>> history(String region) {
		return jdbc.queryForList("select id, action, name, new_name, changed_by, changed_at, row_count, undone_by, undone_at "
				+ "from service_bulk_update where region = ? order by id desc limit 50", region);
	}

	/** Every name on active carers' lists that matches no rota location, or is listed twice. */
	public List<ServiceNameToFix> namesToFix(String region) {
		Set<String> known = knownNames(region);
		Map<String, List<String>> services = new TreeMap<>(), restrictions = new TreeMap<>();
		Set<String> twice = new TreeSet<>();
		for (Map<String, Object> e : jdbc.queryForList("select first_name || ' ' || coalesce(last_name, '') nm, preferred_service, "
				+ "restricted_service from employee where preferred_region = ? and active order by 1", region)) {
			String carer = ((String) e.get("nm")).trim();
			collect((String) e.get("preferred_service"), carer, known, services, twice);
			collect((String) e.get("restricted_service"), carer, known, restrictions, twice);
		}
		// A comma or colon in a house name can never match a list, as both split the list up.
		Set<String> unlistable = new TreeSet<>(known.stream().filter(k -> k.contains(",") || k.contains(":")).toList());
		Set<String> fixedNames = new TreeSet<>(unlistable.stream().map(ServiceBulkUpdateService::withoutPunctuation).toList());

		Set<String> names = new TreeSet<>(services.keySet());
		names.addAll(restrictions.keySet());
		names.removeAll(fixedNames); // cleared by renaming the house below
		List<ServiceNameToFix> out = new ArrayList<>();
		for (String name : names) {
			String problem, action;
			String suggestion = null;
			if (twice.contains(name) && known.contains(name)) {
				problem = "Listed twice";
				suggestion = name;
				action = "MERGE";
			} else {
				List<String> elsewhere = jdbc.queryForList("select distinct region from shift_templates "
						+ "where active and btrim(location) = ? and region <> ?", String.class, name, region);
				suggestion = elsewhere.isEmpty() ? suggest(name, known) : null;
				problem = !elsewhere.isEmpty() ? "House in another region (" + String.join(", ", elsewhere) + ")"
						: suggestion != null ? "Different spelling" : "No such house";
				action = suggestion != null ? "MERGE" : "REMOVE";
			}
			out.add(new ServiceNameToFix(name, problem, suggestion, action, services.getOrDefault(name, List.of()),
					restrictions.getOrDefault(name, List.of())));
		}
		for (String house : unlistable) {
			String clean = withoutPunctuation(house);
			out.add(new ServiceNameToFix(house, "Comma or colon in the house name", clean, "RENAME",
					services.getOrDefault(clean, List.of()), restrictions.getOrDefault(clean, List.of())));
		}
		return out;
	}

	private static String withoutPunctuation(String name) {
		return name.replaceAll("[,:]", " ").replaceAll("\\s+", " ").trim();
	}

	private static void collect(String list, String carer, Set<String> known, Map<String, List<String>> into, Set<String> twice) {
		for (String name : new TreeSet<>(ServiceListEdit.names(list))) {
			boolean repeated = ServiceListEdit.listedTwice(list, name);
			if (repeated) {
				twice.add(name);
			}
			if (repeated || !known.contains(name)) {
				into.computeIfAbsent(name, k -> new ArrayList<>()).add(carer);
			}
		}
	}

	/**
	 * The nearest rota location: same letters, one inside the other, or a small slip of the keys.
	 * Their numbers must match, or FLAT 3 would be offered FLAT 2.
	 */
	static String suggest(String name, Set<String> known) {
		String wanted = comparable(name);
		return known.stream()
				.filter(k -> !k.contains(",") && !k.contains(":"))
				.filter(k -> digits(comparable(k)).equals(digits(wanted)))
				.map(k -> Map.entry(k, closeness(wanted, comparable(k))))
				.filter(e -> e.getValue() <= 3)
				.min(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).thenComparing(Map.Entry::getKey))
				.map(Map.Entry::getKey).orElse(null);
	}

	private static String comparable(String name) {
		return name.toUpperCase().replaceAll("\\s+", " ").trim().replaceAll("\\bFIRST\\b", "1ST");
	}

	private static String digits(String name) {
		return name.replaceAll("\\D", "");
	}

	private static int closeness(String a, String b) {
		if (a.equals(b)) {
			return 0;
		}
		if (a.length() >= 6 && b.length() >= 6 && (a.contains(b) || b.contains(a))) {
			return 1;
		}
		return a.length() >= 6 ? editDistance(a, b) : Integer.MAX_VALUE;
	}

	private static int editDistance(String a, String b) {
		int[] row = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			row[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			int diagonal = row[0];
			row[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int above = row[j];
				row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), diagonal + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
				diagonal = above;
			}
		}
		return row[b.length()];
	}

	/** Checks the request and works out every row it would change. */
	private List<Change> plan(ServiceBulkRequest req) {
		String action = req.action() == null ? "" : req.action();
		if (req.region() == null || req.region().isBlank()) {
			throw new IllegalArgumentException("Choose a region");
		}
		if (req.name() == null || req.name().isBlank()) {
			throw new IllegalArgumentException("Choose a name to change");
		}
		String region = req.region();
		String name = req.name().trim();
		String newName = null;
		Set<String> known = knownNames(region);
		switch (action) {
			case "RENAME" -> {
				newName = checkedNewName(req.newName());
				String target = newName;
				if (known.stream().anyMatch(k -> k.equalsIgnoreCase(target) && !k.equalsIgnoreCase(name))) {
					throw new IllegalArgumentException(target + " already exists. Use merge instead");
				}
				if (target.equals(name)) {
					throw new IllegalArgumentException("Enter a different name");
				}
			}
			case "MERGE" -> {
				newName = checkedNewName(req.newName());
				if (!known.contains(newName)) {
					throw new IllegalArgumentException(newName + " is not a location in " + region);
				}
			}
			case "REMOVE" -> { }
			default -> throw new IllegalArgumentException("Choose rename, merge or remove");
		}
		boolean merging = "MERGE".equals(action) && !name.equals(newName);

		List<Change> changes = new ArrayList<>();
		for (Map<String, Object> e : jdbc.queryForList("select id, first_name || ' ' || coalesce(last_name, '') nm, preferred_service, "
				+ "restricted_service from employee where preferred_region = ?", region)) {
			String id = String.valueOf(e.get("id")), carer = ((String) e.get("nm")).trim();
			for (String column : List.of("preferred_service", "restricted_service")) {
				String list = (String) e.get(column);
				if (name.equals(newName) && !ServiceListEdit.listedTwice(list, name)) {
					continue; // clearing repeats: leave everyone else's list as it is
				}
				String edited = ServiceListEdit.replace(list, name, newName);
				if (!java.util.Objects.equals(list, edited)) {
					changes.add(new Change("employee", id, column, list, edited, carer));
				}
			}
		}
		if ("RENAME".equals(action) || merging) {
			String target = newName;
			jdbc.queryForList("select id, location from shift_templates where region = ? and btrim(location) = ?", region, name)
					.forEach(r -> changes.add(new Change("shift_templates", str(r, "id"), "location", str(r, "location"), target, null)));
			jdbc.queryForList("select p.id, p.location from employee_schedule_patterns p join employee e on e.id = p.employee_id "
					+ "where e.preferred_region = ? and btrim(p.location) = ?", region, name)
					.forEach(r -> changes.add(new Change("employee_schedule_patterns", str(r, "id"), "location", str(r, "location"), target, null)));
			jdbc.queryForList("select id, service from publish_log where btrim(service) = ? and rota_id in "
					+ "(select rota_id from deferred_solve_request where region = ?)", name, region)
					.forEach(r -> changes.add(new Change("publish_log", str(r, "id"), "service", str(r, "service"), target, null)));
			// one row per carer and house: when merging, a carer already at the target keeps that row
			jdbc.queryForList("select h.employee_id, h.location from carer_house_history h join employee e on e.id = h.employee_id "
					+ "where e.preferred_region = ? and btrim(h.location) = ? and not exists (select 1 from carer_house_history t "
					+ "where t.employee_id = h.employee_id and t.location = ?)", region, name, target)
					.forEach(r -> changes.add(new Change("carer_house_history", str(r, "employee_id"), "location", str(r, "location"), target, null)));
			for (Map<String, Object> s : jdbc.queryForList("select id, staffrota_location from service_location "
					+ "where staffrota_region = ? and btrim(staffrota_location) = ?", region, name)) {
				if (merging) {
					Integer flats = jdbc.queryForObject("select count(*) from service_location where parent_id = ? and active",
							Integer.class, s.get("id"));
					if (flats != null && flats > 0) {
						throw new IllegalArgumentException(name + " has flats. Move them first");
					}
					changes.add(new Change("service_location", str(s, "id"), "active", "true", "false", null));
				} else {
					changes.add(new Change("service_location", str(s, "id"), "staffrota_location", str(s, "staffrota_location"), target, null));
				}
			}
		}
		if (changes.isEmpty()) {
			throw new IllegalArgumentException("Nothing to change: no records use " + name);
		}
		return changes;
	}

	private List<String> warnings(ServiceBulkRequest req) {
		if (!"MERGE".equals(req.action()) || req.name().trim().equals(req.newName().trim())) {
			return List.of();
		}
		Integer both = jdbc.queryForObject("select count(distinct btrim(location)) from shift_templates where region = ? and active "
				+ "and btrim(location) in (?, ?)", Integer.class, req.region(), req.name().trim(), req.newName().trim());
		return both != null && both == 2
				? List.of("Both have shift templates. Merging adds them together, so the location could end up with double the shifts.")
				: List.of();
	}

	private static ServiceBulkPreview summarise(List<Change> changes, List<String> warnings) {
		return new ServiceBulkPreview(count(changes, "shift_templates"), carers(changes, "preferred_service"),
				carers(changes, "restricted_service"), count(changes, "service_location"), count(changes, "employee_schedule_patterns"),
				count(changes, "publish_log"), count(changes, "carer_house_history"), warnings);
	}

	private static int count(List<Change> changes, String table) {
		return (int) changes.stream().filter(c -> c.table().equals(table)).count();
	}

	private static List<String> carers(List<Change> changes, String column) {
		return changes.stream().filter(c -> c.column().equals(column)).map(Change::label).sorted().toList();
	}

	private String read(String table, String rowId, String column, String value) {
		return switch (table) {
			case "carer_house_history" -> jdbc.queryForList("select location from carer_house_history where employee_id = ? and location = ?",
					String.class, Long.parseLong(rowId), value).stream().findFirst().orElse(null);
			case "service_location" -> "active".equals(column)
					? String.valueOf(jdbc.queryForObject("select active from service_location where id = ?", Boolean.class, Long.parseLong(rowId)))
					: jdbc.queryForObject("select staffrota_location from service_location where id = ?", String.class, Long.parseLong(rowId));
			default -> jdbc.queryForObject("select " + safeColumn(table, column) + " from " + table + " where id = ?", String.class,
					Long.parseLong(rowId));
		};
	}

	private void write(String table, String rowId, String column, String oldValue, String value) {
		long id = Long.parseLong(rowId);
		if ("carer_house_history".equals(table)) {
			jdbc.update("update carer_house_history set location = ? where employee_id = ? and location = ?", value, id, oldValue);
		} else if ("service_location".equals(table) && "active".equals(column)) {
			jdbc.update("update service_location set active = ? where id = ?", Boolean.valueOf(value), id);
		} else {
			jdbc.update("update " + table + " set " + safeColumn(table, column) + " = ? where id = ?", value, id);
		}
	}

	/** Table and column names can't be bound as parameters, so only these are ever used. */
	private static String safeColumn(String table, String column) {
		boolean allowed = switch (table) {
			case "employee" -> Set.of("preferred_service", "restricted_service").contains(column);
			case "shift_templates", "employee_schedule_patterns" -> "location".equals(column);
			case "publish_log" -> "service".equals(column);
			case "service_location" -> "staffrota_location".equals(column);
			default -> false;
		};
		if (!allowed) {
			throw new IllegalStateException("Unexpected column " + table + "." + column);
		}
		return column;
	}

	private void refuseWhileSolving(String region) {
		Integer pending = jdbc.queryForObject("select count(*) from deferred_solve_request where region = ? and not completed",
				Integer.class, region);
		if (pending != null && pending > 0) {
			throw new IllegalArgumentException("A solve is running or waiting for " + region + ". Try again when it finishes");
		}
		for (Long rotaId : liveSessions.activeRotaIds()) {
			Integer live = jdbc.queryForObject("select count(*) from deferred_solve_request where region = ? and rota_id = ?",
					Integer.class, region, rotaId);
			if (live != null && live > 0) {
				throw new IllegalArgumentException("A live session is open for " + region + ". Try again when it closes");
			}
		}
	}

	private Set<String> knownNames(String region) {
		return new TreeSet<>(jdbc.queryForList("select distinct btrim(location) from shift_templates where region = ? and active "
				+ "union select coalesce(staffrota_location, name) from service_location where staffrota_region = ? and active",
				String.class, region, region));
	}

	private static String checkedNewName(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("Enter the new name");
		}
		String name = raw.trim();
		if (name.contains(",") || name.contains(":")) {
			throw new IllegalArgumentException("A name can't contain a comma or a colon");
		}
		if (name.length() > MAX_NAME) {
			throw new IllegalArgumentException("A name can be at most " + MAX_NAME + " characters");
		}
		return name;
	}

	private static String str(Map<String, Object> row, String key) {
		return String.valueOf(row.get(key));
	}
}
