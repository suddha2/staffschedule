package staffschedule;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftAssignmentFactory;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.FollowerShiftAssignment;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.util.ShiftType;

/**
 * Parity guard for the data-driven pairing (Installment 2): the factory/linker no
 * longer branch on {@code type == SLEEP_IN}, they read the follower flag — which for
 * SLEEP_IN comes from the seeded type default (via ShiftTypeMeta's fallback, so no DB
 * is needed here). This proves the generalised code reproduces the exact
 * LONG_DAY leader ↔ SLEEP_IN follower behaviour.
 */
class ShiftAssignmentFactoryTest {

    private static final LocalDate D = LocalDate.of(2027, 1, 5); // a Monday

    private ShiftTemplate tpl(String location, ShiftType type) {
        ShiftTemplate t = new ShiftTemplate();
        t.setLocation(location);
        t.setShiftType(type);
        t.setDayOfWeek(DayOfWeek.MONDAY);
        t.setStartTime(LocalTime.of(8, 0));
        t.setEndTime(LocalTime.of(20, 0));
        t.setEmpCount(1);
        return t;
    }

    @Test
    void sleepInBecomesFollowerAndPairsToItsLongDay() {
        Shift longDay = new Shift(D, tpl("Riverdale", ShiftType.LONG_DAY), 1);
        Shift sleepIn = new Shift(D, tpl("Riverdale", ShiftType.SLEEP_IN), 1);

        ShiftAssignment ld = ShiftAssignmentFactory.create(longDay);
        ShiftAssignment si = ShiftAssignmentFactory.create(sleepIn);

        // Follower → shadow entity; leader → genuine work entity (was: type == SLEEP_IN).
        assertInstanceOf(WorkShiftAssignment.class, ld, "LONG_DAY is a genuine work assignment");
        assertInstanceOf(FollowerShiftAssignment.class, si, "SLEEP_IN is the follower/shadow assignment");

        ShiftAssignmentFactory.linkFollowerPairs(List.of(ld, si));

        assertSame(si, ((WorkShiftAssignment) ld).getPairedFollower(),
                "the long-day leader is linked to its sleep-in follower, same location + date");
    }

    @Test
    void nonPairingTypesGetNoPairIdAndNoLink() {
        Shift day = new Shift(D, tpl("Riverdale", ShiftType.DAY), 1);
        Shift waking = new Shift(D, tpl("Riverdale", ShiftType.WAKING_NIGHT), 1);

        assertNull(day.getPairId(), "DAY does not participate in pairing");
        assertNull(waking.getPairId(), "WAKING_NIGHT does not participate in pairing");

        ShiftAssignment a = ShiftAssignmentFactory.create(day);
        assertInstanceOf(WorkShiftAssignment.class, a);
        // Nothing to link; must not throw.
        ShiftAssignmentFactory.linkFollowerPairs(List.of(a, ShiftAssignmentFactory.create(waking)));
    }

    @Test
    void twoToOneLongDaysZipToTwoSleepIns() {
        Shift ld1 = new Shift(D, tpl("Audley", ShiftType.LONG_DAY), 1);
        Shift ld2 = new Shift(D, tpl("Audley", ShiftType.LONG_DAY), 1);
        Shift si1 = new Shift(D, tpl("Audley", ShiftType.SLEEP_IN), 1);
        Shift si2 = new Shift(D, tpl("Audley", ShiftType.SLEEP_IN), 1);

        ShiftAssignment a1 = ShiftAssignmentFactory.create(ld1);
        ShiftAssignment a2 = ShiftAssignmentFactory.create(ld2);
        ShiftAssignment b1 = ShiftAssignmentFactory.create(si1);
        ShiftAssignment b2 = ShiftAssignmentFactory.create(si2);

        ShiftAssignmentFactory.linkFollowerPairs(List.of(a1, a2, b1, b2));

        // Each leader gets a distinct follower (position zip within the location+date group).
        FollowerShiftAssignment f1 = ((WorkShiftAssignment) a1).getPairedFollower();
        FollowerShiftAssignment f2 = ((WorkShiftAssignment) a2).getPairedFollower();
        assertInstanceOf(FollowerShiftAssignment.class, f1);
        assertInstanceOf(FollowerShiftAssignment.class, f2);
        org.junit.jupiter.api.Assertions.assertNotSame(f1, f2, "the two long-days pair to different sleep-ins");
    }
}
