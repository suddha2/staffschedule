package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import com.midco.rota.model.ConstraintSetting;
import com.midco.rota.model.Rota;
import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.opt.SolverTuning;
import com.midco.rota.util.ConstraintType;

/**
 * Pure tests for the database-driven constraint weights — no Spring context, no
 * database. Building the solver is the real check: OptaPlanner constructs the
 * constraint streams at that point and fails if any constraint using
 * {@code penalizeConfigurable} has no matching {@code @ConstraintWeight}, or if
 * the constraint package on {@link RotaConstraintConfiguration} does not line up
 * with the provider.
 */
class RotaConstraintConfigurationTest {

	@Test
	void solverBuildsWithConfigurableConstraintWeights() {
		SolverConfig config = SolverConfig.createFromXmlResource("solverConfig.xml");
		SolverFactory<Rota> factory = SolverFactory.create(config);

		assertNotNull(factory.buildSolver(),
				"Solver must build: this proves every configurable constraint resolves to a @ConstraintWeight");
	}

	@Test
	void constraintWeightNamesAreUniqueAndNonBlank() {
		List<String> names = new ArrayList<>();
		for (Field field : RotaConstraintConfiguration.class.getDeclaredFields()) {
			ConstraintWeight weight = field.getAnnotation(ConstraintWeight.class);
			if (weight != null) {
				assertTrue(weight.value() != null && !weight.value().isBlank(),
						"Constraint weight on " + field.getName() + " must name a constraint");
				names.add(weight.value());
			}
		}
		assertEquals(names.size(), names.stream().distinct().count(),
				"Two @ConstraintWeight fields name the same constraint");
		assertEquals(44, names.size(),
				"Every constraint in defineConstraints needs a weight, including the inactive ones");
	}

	/**
	 * The two rules the gap analysis demoted must carry no hard component. If
	 * either regains one, periods become unsolvable again whenever real staffing
	 * does not fit the 5-or-6 shifts-per-week window.
	 */
	@Test
	void contractualWeeklyRulesAreSoftNotHard() {
		RotaConstraintConfiguration configuration = new RotaConstraintConfiguration();

		assertEquals(0L, configuration.getPermanentWeeklyCap().hardScore(),
				"Permanent weekly shift cap must be soft");
		assertTrue(configuration.getPermanentWeeklyCap().softScore() < 0L
				|| configuration.getPermanentWeeklyCap().softScore() > 0L,
				"Permanent weekly shift cap still needs a soft weight to shape the solution");

		assertEquals(0L, configuration.getPermanentWeeklyMinimum().hardScore(),
				"Permanent weekly shift minimum must be soft");
	}

	/** A zero weight is how a constraint gets switched off from the database. */
	@Test
	void zeroWeightIsRepresentable() {
		RotaConstraintConfiguration configuration = new RotaConstraintConfiguration();
		configuration.setGenderMismatch(HardSoftLongScore.ZERO);

		assertEquals(0L, configuration.getGenderMismatch().hardScore());
		assertEquals(0L, configuration.getGenderMismatch().softScore());
	}

	@Test
	void tuningDefaultsMatchTheGapAnalysis() {
		SolverTuning tuning = new SolverTuning();

		assertEquals(6, tuning.getWeeklyCapOddWeek());
		assertEquals(5, tuning.getWeeklyCapEvenWeek());
		assertEquals(6, tuning.capForWeekOfPeriod(1), "Weeks 1 and 3 take the odd cap");
		assertEquals(5, tuning.capForWeekOfPeriod(2), "Weeks 2 and 4 take the even cap");
		assertEquals(10, tuning.getMaxNonFloatingShiftsPerLocationPerWeek(), "Widened from 4");
		assertEquals(6, tuning.getMaxLocationsPerPeriod(), "Widened from 3");
	}

	/**
	 * The settings table stores one weight plus a HARD/SOFT type, so a default
	 * that mixed the two could not be seeded. Nothing currently does, and this
	 * guards against someone introducing one.
	 */
	@Test
	void everyDefaultIsPurelyHardOrPurelySoft() {
		RotaConstraintConfiguration configuration = new RotaConstraintConfiguration();
		List<String> mixed = new ArrayList<>();

		for (Field field : RotaConstraintConfiguration.class.getDeclaredFields()) {
			ConstraintWeight weight = field.getAnnotation(ConstraintWeight.class);
			if (weight == null) {
				continue;
			}
			field.setAccessible(true);
			HardSoftLongScore score;
			try {
				score = (HardSoftLongScore) field.get(configuration);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("Could not read " + field.getName(), e);
			}
			if (score.hardScore() != 0L && score.softScore() != 0L) {
				mixed.add(weight.value() + " = " + score);
			}
		}
		assertTrue(mixed.isEmpty(), "These defaults mix hard and soft, which constraint_setting cannot store: " + mixed);
	}

	/**
	 * Every name in the inactive set must be a real constraint. A typo here would
	 * seed the constraint active by mistake, which for "Missing required skill"
	 * would silently start enforcing a rule that has never been in force.
	 */
	@Test
	void inactiveByDefaultNamesAllExist() {
		List<String> known = new ArrayList<>();
		for (Field field : RotaConstraintConfiguration.class.getDeclaredFields()) {
			ConstraintWeight weight = field.getAnnotation(ConstraintWeight.class);
			if (weight != null) {
				known.add(weight.value());
			}
		}
		for (String name : RotaConstraintConfiguration.INACTIVE_BY_DEFAULT) {
			assertTrue(known.contains(name), "INACTIVE_BY_DEFAULT names a constraint that does not exist: " + name);
		}
		assertEquals(13, RotaConstraintConfiguration.INACTIVE_BY_DEFAULT.size(),
				"13 constraints were defined but never registered before this change");
	}

	@Test
	void settingRoundTripsThroughWeightAndType() {
		ConstraintSetting hard = ConstraintSetting.fromScore("Gender mismatch", HardSoftLongScore.ofHard(1), "d");
		assertEquals(ConstraintType.HARD, hard.getConstraintType());
		assertEquals(1L, hard.getScoreWeight());
		assertEquals(HardSoftLongScore.ofHard(1), hard.toScore());

		ConstraintSetting soft = ConstraintSetting.fromScore("Unassigned shift", HardSoftLongScore.ofSoft(1_000_000),
				"d");
		assertEquals(ConstraintType.SOFT, soft.getConstraintType());
		assertEquals(1_000_000L, soft.getScoreWeight());
		assertEquals(HardSoftLongScore.ofSoft(1_000_000), soft.toScore());
	}

	@Test
	void disablingASettingZeroesItWithoutLosingTheConfiguration() {
		ConstraintSetting setting = ConstraintSetting.fromScore("Gender mismatch", HardSoftLongScore.ofHard(7), "d");
		setting.setEnabled(false);

		assertEquals(HardSoftLongScore.ZERO, setting.toScore(), "A disabled constraint contributes nothing");
		assertEquals(ConstraintType.HARD, setting.getConstraintType(), "Type survives so re-enabling restores it");
		assertEquals(7L, setting.getScoreWeight(), "Weight survives so re-enabling restores it");
	}

	@Test
	void mixedScoresAreRejectedRatherThanSilentlyTruncated() {
		assertNull(ConstraintSetting.fromScore("nonsense", HardSoftLongScore.of(3, 500), "d"),
				"A mixed score has no single weight-plus-type representation and must not be half-stored");
	}

	@Test
	void tuningIsSwappableAtRuntime() {
		SolverTuning original = SolverTuning.current();
		try {
			SolverTuning replacement = new SolverTuning();
			replacement.setWeeklyCapOddWeek(9);
			SolverTuning.apply(replacement);

			assertEquals(9, SolverTuning.current().getWeeklyCapOddWeek());
			assertEquals(9, SolverTuning.current().capForWeekOfPeriod(3));
		} finally {
			SolverTuning.apply(original);
		}
	}
}
