package staffschedule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.midco.rota.SolverTrigger;
import com.midco.rota.model.FollowerShiftAssignment;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftAssignmentFactory;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.ShiftTypeDef;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.opt.ShiftTypeMeta;

/**
 * Leader/follower pairing is type-level and data-driven: any type can lead and any other
 * can follow it (set in Manage Shift Types), not just LONG_DAY → SLEEP_IN.
 */
class CustomPairingTest {

	private static final LocalDate D = LocalDate.of(2027, 1, 4); // a Monday

	private static ShiftTypeDef type(String code, String follows) {
		ShiftTypeDef d = new ShiftTypeDef();
		d.setCode(code);
		d.setActive(true);
		d.setDefaultIsFollower(follows != null);
		d.setDefaultPairsWith(follows);
		return d;
	}

	@BeforeEach
	void configureTypes() {
		ShiftTypeMeta.load(List.of(
				type("LONG_DAY", null), type("SLEEP_IN", "LONG_DAY"),
				type("TWILIGHT", null), type("NIGHT_COVER", "TWILIGHT"), // a brand-new pair
				type("DAY", null), type("ORPHAN_FOLLOWER", "NO_SUCH_TYPE")));
	}

	@AfterEach
	void resetTypes() {
		ShiftTypeMeta.load(List.of()); // back to the built-in fallback
	}

	private static Shift shift(String location, String code) {
		ShiftTemplate t = new ShiftTemplate();
		t.setLocation(location);
		t.setShiftTypeCode(code);
		t.setDayOfWeek(DayOfWeek.MONDAY);
		t.setStartTime(LocalTime.of(8, 0));
		t.setEndTime(LocalTime.of(20, 0));
		t.setEmpCount(1);
		return new Shift(D, t, 1);
	}

	private static ShiftAssignment of(List<ShiftAssignment> all, Shift s) {
		return all.stream().filter(sa -> sa.getShift() == s).findFirst().orElseThrow();
	}

	@Test
	void eachFollowerPairsOnlyWithItsOwnLeaderType() {
		Shift ld = shift("House", "LONG_DAY"), si = shift("House", "SLEEP_IN");
		Shift tw = shift("House", "TWILIGHT"), nc = shift("House", "NIGHT_COVER");
		// Followers listed before leaders and interleaved, so position-zipping across types would cross them.
		List<ShiftAssignment> all = ShiftAssignmentFactory.createAll(List.of(nc, si, tw, ld));
		ShiftAssignmentFactory.linkFollowerPairs(all);

		assertSame(of(all, si), ((WorkShiftAssignment) of(all, ld)).getPairedFollower(), "SLEEP_IN follows LONG_DAY");
		assertSame(of(all, nc), ((WorkShiftAssignment) of(all, tw)).getPairedFollower(), "NIGHT_COVER follows TWILIGHT");
	}

	@Test
	void newFollowerWithoutItsLeaderOnTheDayGetsItsOwnCarer() {
		Shift ld = shift("House", "LONG_DAY"), nc = shift("House", "NIGHT_COVER");
		List<ShiftAssignment> all = ShiftAssignmentFactory.createAll(List.of(ld, nc));
		ShiftAssignmentFactory.linkFollowerPairs(all);

		assertInstanceOf(WorkShiftAssignment.class, of(all, nc), "no TWILIGHT that day → own carer");
		assertNull(((WorkShiftAssignment) of(all, ld)).getPairedFollower(), "LONG_DAY must not take a NIGHT_COVER");
	}

	@Test
	void followerOfAnUnconfiguredLeaderGetsItsOwnCarer() {
		Shift o = shift("House", "ORPHAN_FOLLOWER");
		List<ShiftAssignment> all = ShiftAssignmentFactory.createAll(List.of(shift("House", "DAY"), o));
		assertInstanceOf(WorkShiftAssignment.class, of(all, o));
	}

	@Test
	void pairedFollowerStaysAShadowOfItsNewLeader() {
		Shift tw = shift("House", "TWILIGHT"), nc = shift("House", "NIGHT_COVER");
		List<ShiftAssignment> all = ShiftAssignmentFactory.createAll(List.of(tw, nc));
		assertInstanceOf(FollowerShiftAssignment.class, of(all, nc));
	}

	@Test
	void pinsAllowAnyConfiguredPairOnTheSameDay() {
		assertTrue(SolverTrigger.isAllowedDayShiftCodes(List.of("TWILIGHT", "NIGHT_COVER")));
		assertTrue(SolverTrigger.isAllowedDayShiftCodes(List.of("SLEEP_IN", "LONG_DAY")));
		assertFalse(SolverTrigger.isAllowedDayShiftCodes(List.of("LONG_DAY", "NIGHT_COVER")), "not a configured pair");
		assertFalse(SolverTrigger.isAllowedDayShiftCodes(List.of("DAY", "TWILIGHT")));
		assertFalse(SolverTrigger.isAllowedDayShiftCodes(List.of("FLOATING", "TWILIGHT")));
		assertTrue(SolverTrigger.isAllowedDayShiftCodes(List.of("FLOATING", "FLOATING")));
	}
}
