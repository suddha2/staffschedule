package com.midco.rota.integration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.model.EmployeeAvailability;
import com.midco.rota.repository.EmployeeAvailabilityRepository;
import com.midco.rota.util.AvailabilitySource;
import com.midco.rota.util.AvailabilityType;

/**
 * Daily job that pulls booked leave / unavailability from People Planner and
 * upserts it into {@code employee_availability}, so the solver's
 * "Employee unavailable (leave)" hard constraint never allocates onto leave.
 *
 * <p>Idempotent: each PP record is matched by ({@code PP_API}, external ref) and
 * updated in place, so re-running never duplicates. A record flagged cancelled
 * in PP is deleted locally. Runs on {@code peopleplanner.sync-cron}; does nothing
 * while the integration is disabled.
 */
@Service
public class LeaveSyncService {

	private static final Logger logger = LoggerFactory.getLogger(LeaveSyncService.class);

	private final DataEngineClient dataEngineClient;
	private final EmployeeAvailabilityRepository availabilityRepository;
	private final PeoplePlannerProperties props;

	public LeaveSyncService(DataEngineClient dataEngineClient,
			EmployeeAvailabilityRepository availabilityRepository, PeoplePlannerProperties props) {
		this.dataEngineClient = dataEngineClient;
		this.availabilityRepository = availabilityRepository;
		this.props = props;
	}

	/** Scheduled entry point. Cron comes from {@code peopleplanner.sync-cron}. */
	@Scheduled(cron = "${peopleplanner.sync-cron:0 30 3 * * *}")
	public void scheduledSync() {
		if (!props.isEnabled()) {
			return;
		}
		LocalDate from = LocalDate.now().minusDays(props.getLookbackDays());
		LocalDate to = LocalDate.now().plusDays(props.getLookaheadDays());
		syncLeave(from, to);
	}

	/**
	 * Pull the window from PP and reconcile it into the table. Package-visible so
	 * an admin endpoint can trigger an on-demand sync.
	 *
	 * @return number of records applied (inserted or updated)
	 */
	@Transactional
	public int syncLeave(LocalDate from, LocalDate to) {
		List<PpLeaveRecord> records = dataEngineClient.fetchLeave(from, to);
		if (records.isEmpty()) {
			logger.info("Leave sync {}..{}: no records returned (disabled, empty, or fetch failed)", from, to);
			return 0;
		}

		int applied = 0;
		int deleted = 0;
		for (PpLeaveRecord r : records) {
			if (r.getExternalRef() == null || r.getEmployeeId() == null
					|| r.getStartDate() == null || r.getEndDate() == null) {
				logger.warn("Skipping malformed PP leave record (ref={}, emp={})", r.getExternalRef(), r.getEmployeeId());
				continue;
			}

			EmployeeAvailability existing =
					availabilityRepository.findBySourceAndExternalRef(AvailabilitySource.PP_API, r.getExternalRef());

			if (r.isCancelled()) {
				if (existing != null) {
					availabilityRepository.delete(existing);
					deleted++;
				}
				continue;
			}

			EmployeeAvailability row = (existing != null) ? existing : new EmployeeAvailability();
			row.setEmployeeId(r.getEmployeeId());
			row.setStartDate(r.getStartDate());
			row.setEndDate(r.getEndDate());
			row.setType(mapType(r.getType()));
			row.setSource(AvailabilitySource.PP_API);
			row.setExternalRef(r.getExternalRef());
			row.setReason(r.getReason());
			row.setSyncedAt(LocalDateTime.now());
			availabilityRepository.save(row);
			applied++;
		}

		logger.info("Leave sync {}..{}: {} applied, {} cancelled/removed", from, to, applied, deleted);
		return applied;
	}

	/** Map PP's free-text category to our enum; unknown or null falls back to planned leave. */
	private AvailabilityType mapType(String ppType) {
		if (ppType == null) {
			return AvailabilityType.PLANNED_LEAVE;
		}
		String t = ppType.trim().toUpperCase();
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
