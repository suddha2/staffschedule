package com.midco.rota.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.model.ConstraintSetting;
import com.midco.rota.model.SolverTuningSetting;
import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.opt.SolverTuning;
import com.midco.rota.repository.ConstraintSettingRepository;
import com.midco.rota.repository.SolverTuningSettingRepository;

import jakarta.annotation.PostConstruct;

/**
 * Loads solver configuration from the database: per-constraint weights and
 * severity into a {@link RotaConstraintConfiguration}, and numeric thresholds
 * into {@link SolverTuning}.
 *
 * <p>Seeding is incremental rather than all-or-nothing. On every start, any
 * constraint or threshold that has no row yet gets one carrying the code
 * default, so adding a constraint in code makes its row appear on the next
 * restart without a migration. Existing rows are never overwritten — once a row
 * exists, the database is authoritative.
 *
 * <p>Every database access is defensive. The schema is managed by hand
 * ({@code ddl-auto=none}), so an instance may be running before the migration
 * has been applied. In that case this service logs a warning and falls back to
 * the code defaults rather than preventing the application from starting.
 */
@Service
public class SolverConfigService {

	private static final Logger logger = LoggerFactory.getLogger(SolverConfigService.class);

	private static final String SEEDED_ACTIVE = "Seeded from code default";
	private static final String SEEDED_INACTIVE = "Never registered before; seeded inactive to preserve behaviour";

	/** Threshold keys, paired with how to read and write them on {@link SolverTuning}. */
	private static final Map<String, TuningAccessor> TUNING = buildTuningAccessors();

	@Autowired
	private ConstraintSettingRepository constraintSettingRepository;

	@Autowired
	private SolverTuningSettingRepository solverTuningSettingRepository;

	// ------------------------------------------------------------------ startup

	@PostConstruct
	public void init() {
		try {
			seedMissingConstraintSettings();
			seedMissingTuning();
			refreshTuning();
		} catch (Exception e) {
			logger.warn("Solver configuration could not be loaded from the database; "
					+ "falling back to code defaults. Has the constraint_setting / solver_tuning "
					+ "migration been applied? Cause: {}", e.toString());
		}
	}

	// ------------------------------------------------------------------ weights

	/**
	 * Builds the constraint configuration for a solve: code defaults first, then
	 * whatever {@code constraint_setting} overrides. A row with
	 * {@code enabled = false}, or a zero weight, yields
	 * {@link HardSoftLongScore#ZERO} — OptaPlanner then skips that constraint
	 * entirely when it builds the scoring session.
	 *
	 * <p>A constraint listed in
	 * {@link RotaConstraintConfiguration#INACTIVE_BY_DEFAULT} with no row at all
	 * also resolves to zero, so deleting a row cannot silently switch on a rule
	 * that has never been in force.
	 */
	public RotaConstraintConfiguration buildConstraintConfiguration() {
		RotaConstraintConfiguration configuration = new RotaConstraintConfiguration();
		Map<String, ConstraintSetting> rows;
		try {
			rows = new HashMap<>();
			for (ConstraintSetting setting : constraintSettingRepository.findAll()) {
				rows.put(setting.getConstraintName(), setting);
			}
		} catch (Exception e) {
			logger.warn("Could not read constraint_setting; using code defaults. Cause: {}", e.toString());
			return configuration;
		}

		int fromDatabase = 0;
		int disabled = 0;
		for (Field field : RotaConstraintConfiguration.class.getDeclaredFields()) {
			ConstraintWeight weight = field.getAnnotation(ConstraintWeight.class);
			if (weight == null) {
				continue;
			}
			ConstraintSetting row = rows.get(weight.value());
			HardSoftLongScore score;
			if (row != null) {
				score = row.toScore();
				fromDatabase++;
			} else if (RotaConstraintConfiguration.INACTIVE_BY_DEFAULT.contains(weight.value())) {
				score = HardSoftLongScore.ZERO;
			} else {
				continue;
			}
			if (score.equals(HardSoftLongScore.ZERO)) {
				disabled++;
			}
			try {
				field.setAccessible(true);
				field.set(configuration, score);
			} catch (ReflectiveOperationException e) {
				logger.warn("Could not apply weight for constraint '{}': {}", weight.value(), e.toString());
			}
		}
		logger.info("Solver constraint weights: {} loaded from the database, {} of them switched off",
				fromDatabase, disabled);
		return configuration;
	}

	/**
	 * Inserts a row for any constraint that does not have one yet, carrying the
	 * code default. Constraints listed in
	 * {@link RotaConstraintConfiguration#INACTIVE_BY_DEFAULT} are seeded disabled.
	 * Safe to run on every start; existing rows are left alone.
	 */
	@Transactional
	public void seedMissingConstraintSettings() {
		Set<String> existing = new HashSet<>();
		for (ConstraintSetting setting : constraintSettingRepository.findAll()) {
			existing.add(setting.getConstraintName());
		}

		RotaConstraintConfiguration defaults = new RotaConstraintConfiguration();
		List<ConstraintSetting> seed = new ArrayList<>();
		int inactive = 0;
		for (Field field : RotaConstraintConfiguration.class.getDeclaredFields()) {
			ConstraintWeight weight = field.getAnnotation(ConstraintWeight.class);
			if (weight == null || existing.contains(weight.value())) {
				continue;
			}
			try {
				field.setAccessible(true);
				HardSoftLongScore score = (HardSoftLongScore) field.get(defaults);
				boolean active = !RotaConstraintConfiguration.INACTIVE_BY_DEFAULT.contains(weight.value());
				ConstraintSetting setting = ConstraintSetting.fromScore(weight.value(), score,
						active ? SEEDED_ACTIVE : SEEDED_INACTIVE);
				if (setting == null) {
					logger.warn("Constraint '{}' has a mixed hard/soft default ({}), which a single "
							+ "weight plus type cannot represent; it keeps the code default and gets no row",
							weight.value(), score);
					continue;
				}
				setting.setEnabled(active);
				if (!active) {
					inactive++;
				}
				seed.add(setting);
			} catch (ReflectiveOperationException e) {
				logger.warn("Could not seed constraint '{}': {}", weight.value(), e.toString());
			}
		}

		if (seed.isEmpty()) {
			return;
		}
		constraintSettingRepository.saveAll(seed);
		logger.info("Seeded constraint_setting with {} new rows ({} of them inactive)", seed.size(), inactive);
	}

	// ------------------------------------------------------------------ thresholds

	/** Reads {@code solver_tuning} into the process-wide {@link SolverTuning} holder. */
	public void refreshTuning() {
		SolverTuning tuning = new SolverTuning();
		int applied = 0;
		try {
			for (SolverTuningSetting row : solverTuningSettingRepository.findAll()) {
				TuningAccessor accessor = TUNING.get(row.getSettingKey());
				if (accessor == null) {
					logger.warn("Unknown solver_tuning key '{}' ignored", row.getSettingKey());
					continue;
				}
				accessor.setter.accept(tuning, row.getIntValue());
				applied++;
			}
		} catch (Exception e) {
			logger.warn("Could not read solver_tuning; using code defaults. Cause: {}", e.toString());
			return;
		}
		SolverTuning.apply(tuning);
		logger.info("Solver tuning: {} of {} thresholds loaded from the database", applied, TUNING.size());
	}

	/** Inserts a row for any threshold that does not have one yet. Safe to run repeatedly. */
	@Transactional
	public void seedMissingTuning() {
		Set<String> existing = new HashSet<>();
		for (SolverTuningSetting row : solverTuningSettingRepository.findAll()) {
			existing.add(row.getSettingKey());
		}

		SolverTuning defaults = new SolverTuning();
		List<SolverTuningSetting> seed = new ArrayList<>();
		for (Map.Entry<String, TuningAccessor> entry : TUNING.entrySet()) {
			if (existing.contains(entry.getKey())) {
				continue;
			}
			seed.add(new SolverTuningSetting(entry.getKey(), entry.getValue().getter.applyAsInt(defaults),
					entry.getValue().description));
		}

		if (seed.isEmpty()) {
			return;
		}
		solverTuningSettingRepository.saveAll(seed);
		logger.info("Seeded solver_tuning with {} new rows", seed.size());
	}

	// ------------------------------------------------------------------ plumbing

	private static Map<String, TuningAccessor> buildTuningAccessors() {
		Map<String, TuningAccessor> map = new LinkedHashMap<>();
		map.put("weeklyCapOddWeek", new TuningAccessor(SolverTuning::getWeeklyCapOddWeek,
				SolverTuning::setWeeklyCapOddWeek,
				"Soft target: capped shifts allowed in weeks 1 and 3 of the period"));
		map.put("weeklyCapEvenWeek", new TuningAccessor(SolverTuning::getWeeklyCapEvenWeek,
				SolverTuning::setWeeklyCapEvenWeek,
				"Soft target: capped shifts allowed in weeks 2 and 4 of the period"));
		map.put("weeklyMinPerAllocatedWeek", new TuningAccessor(SolverTuning::getWeeklyMinPerAllocatedWeek,
				SolverTuning::setWeeklyMinPerAllocatedWeek,
				"Soft target: minimum capped shifts in a week the employee works at all"));
		map.put("maxNonFloatingShiftsPerLocationPerWeek",
				new TuningAccessor(SolverTuning::getMaxNonFloatingShiftsPerLocationPerWeek,
						SolverTuning::setMaxNonFloatingShiftsPerLocationPerWeek,
						"Ceiling on non-FLOATING shifts at one location in one week"));
		map.put("maxDaysPerLocationPerWeek", new TuningAccessor(SolverTuning::getMaxDaysPerLocationPerWeek,
				SolverTuning::setMaxDaysPerLocationPerWeek,
				"Soft ceiling on distinct working days at one location in one week"));
		map.put("maxLocationsPerPeriod", new TuningAccessor(SolverTuning::getMaxLocationsPerPeriod,
				SolverTuning::setMaxLocationsPerPeriod, "Ceiling on distinct locations per employee per period"));
		map.put("monthlyHoursCap", new TuningAccessor(SolverTuning::getMonthlyHoursCap,
				SolverTuning::setMonthlyHoursCap, "Ceiling on monthly hours, excluding exempt shift types"));
		map.put("freeLocationsPerWeek", new TuningAccessor(SolverTuning::getFreeLocationsPerWeek,
				SolverTuning::setFreeLocationsPerWeek,
				"Locations per week before the soft switching penalty starts"));
		map.put("minRestHours", new TuningAccessor(SolverTuning::getMinRestHours,
				SolverTuning::setMinRestHours,
				"Minimum hours' rest between a carer's consecutive shifts; 0 = rule off"));
		return map;
	}

	/** How to read and write one threshold, plus the text seeded into its description column. */
	private static final class TuningAccessor {
		private final ToIntFunction<SolverTuning> getter;
		private final ObjIntConsumer<SolverTuning> setter;
		private final String description;

		private TuningAccessor(ToIntFunction<SolverTuning> getter, ObjIntConsumer<SolverTuning> setter,
				String description) {
			this.getter = getter;
			this.setter = setter;
			this.description = description;
		}
	}
}
