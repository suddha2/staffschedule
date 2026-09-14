package com.midco.rota.integration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeAvailability;
import com.midco.rota.repository.EmployeeAvailabilityRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.util.AvailabilitySource;
import com.midco.rota.util.AvailabilityType;

/**
 * Daily job that pulls booked leave / unavailability from every configured
 * {@link LeaveSource} — People Planner and PeopleHR today — and upserts it into
 * {@code employee_availability}, so the solver's "Employee unavailable (leave)"
 * constraint never allocates onto leave.
 *
 * <p>Each source's rows are keyed by (its {@code source}, external ref), so the
 * two feeds never collide and re-running is idempotent. A record a source flags
 * cancelled is deleted locally. Sources that are disabled or unreachable
 * contribute nothing and never remove existing rows.
 */
@Service
public class LeaveSyncService {

	private static final Logger logger = LoggerFactory.getLogger(LeaveSyncService.class);

	private final List<LeaveSource> sources;
	private final EmployeeAvailabilityRepository availabilityRepository;
	private final EmployeeRepository employeeRepository;
	private final PeoplePlannerProperties ppProps; // supplies the shared window/cadence knobs

	public LeaveSyncService(List<LeaveSource> sources,
			EmployeeAvailabilityRepository availabilityRepository, EmployeeRepository employeeRepository,
			PeoplePlannerProperties ppProps) {
		this.sources = sources;
		this.availabilityRepository = availabilityRepository;
		this.employeeRepository = employeeRepository;
		this.ppProps = ppProps;
	}

	/** Scheduled entry point. Cron comes from {@code peopleplanner.sync-cron}. */
	@Scheduled(cron = "${peopleplanner.sync-cron:0 30 3 * * *}")
	public void scheduledSync() {
		LocalDate from = LocalDate.now().minusDays(ppProps.getLookbackDays());
		LocalDate to = LocalDate.now().plusDays(ppProps.getLookaheadDays());
		syncAll(from, to);
	}

	/**
	 * Sync every enabled source for [from, to]. Package-visible so an admin
	 * endpoint could trigger it on demand.
	 *
	 * @return total records applied across all sources
	 */
	public int syncAll(LocalDate from, LocalDate to) {
		int total = 0;
		for (LeaveSource source : sources) {
			if (!source.isEnabled()) {
				continue;
			}
			total += syncOne(source, from, to);
		}
		return total;
	}

	/** Pull one source's window and reconcile it into the table under that source's tag. */
	@Transactional
	public int syncOne(LeaveSource source, LocalDate from, LocalDate to) {
		AvailabilitySource tag = source.source();
		List<LeaveRecord> records = source.fetch(from, to);
		if (records.isEmpty()) {
			logger.info("Leave sync [{}] {}..{}: no records (disabled, empty, or fetch failed)", tag, from, to);
			return 0;
		}

		// Build the match maps once. Primary: this source's own employee id, held on
		// the employee record (pp_employee_id for PP, peoplehr_employee_id for HR).
		// Fallback: email, for employees whose external id is not populated yet.
		Map<String, Integer> idByExternalRef = new HashMap<>();
		Map<String, Integer> idByEmail = new HashMap<>();
		for (Employee e : employeeRepository.findAll()) {
			String ext = (tag == AvailabilitySource.HR_API) ? e.getPeopleHrEmployeeId() : e.getPpEmployeeId();
			if (ext != null && !ext.isBlank()) {
				idByExternalRef.put(ext.trim(), e.getId());
			}
			if (e.getEmail() != null && !e.getEmail().isBlank()) {
				idByEmail.put(e.getEmail().trim().toLowerCase(Locale.ROOT), e.getId());
			}
		}

		int applied = 0;
		int deleted = 0;
		int unmatched = 0;
		for (LeaveRecord r : records) {
			if (!r.isValid()) {
				logger.warn("[{}] skipping malformed leave record (ref={})", tag, r.externalRef());
				continue;
			}
			Integer liveId = null;
			if (r.sourceEmployeeId() != null && !r.sourceEmployeeId().isBlank()) {
				liveId = idByExternalRef.get(r.sourceEmployeeId().trim());
			}
			if (liveId == null && r.employeeEmail() != null && !r.employeeEmail().isBlank()) {
				liveId = idByEmail.get(r.employeeEmail().trim().toLowerCase(Locale.ROOT));
			}
			if (liveId == null) {
				unmatched++;
				logger.warn("[{}] leave for unresolved employee (sourceId={}, email={}, ref={}) — skipped",
						tag, r.sourceEmployeeId(), r.employeeEmail(), r.externalRef());
				continue;
			}
			EmployeeAvailability existing = availabilityRepository.findBySourceAndExternalRef(tag, r.externalRef());

			if (r.cancelled()) {
				if (existing != null) {
					availabilityRepository.delete(existing);
					deleted++;
				}
				continue;
			}

			EmployeeAvailability row = (existing != null) ? existing : new EmployeeAvailability();
			row.setEmployeeId(liveId);
			row.setStartDate(r.startDate());
			row.setEndDate(r.endDate());
			row.setType(mapType(r.type()));
			row.setSource(tag);
			row.setExternalRef(r.externalRef());
			row.setReason(r.reason());
			row.setSyncedAt(LocalDateTime.now());
			availabilityRepository.save(row);
			applied++;
		}

		logger.info("Leave sync [{}] {}..{}: {} applied, {} cancelled/removed, {} unmatched email(s)",
				tag, from, to, applied, deleted, unmatched);
		return applied;
	}

	/** Map a source's free-text category to our enum; unknown/null falls back to planned leave. */
	private AvailabilityType mapType(String raw) {
		if (raw == null) {
			return AvailabilityType.PLANNED_LEAVE;
		}
		String t = raw.trim().toUpperCase();
		if (t.contains("SICK")) {
			return AvailabilityType.SICK;
		}
		if (t.contains("TRAIN")) {
			return AvailabilityType.TRAINING;
		}
		if (t.contains("LEAVE") || t.contains("HOLIDAY") || t.contains("ANNUAL")) {
			return AvailabilityType.PLANNED_LEAVE;
		}
		return AvailabilityType.UNAVAILABLE;
	}
}
