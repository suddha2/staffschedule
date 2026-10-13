package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.opt.RotaConstraintProvider;
import com.midco.rota.util.Gender;

/**
 * A template with no gender set must mean "any carer". It used to be scored as a HARD
 * mismatch for every carer, which left whole template rows empty (Barnet rota 464).
 */
class GenderRuleTest {

	private Employee carer(Gender g) {
		Employee e = new Employee();
		e.setGender(g);
		return e;
	}

	@Test
	void noGenderSetIsNotAMismatch() {
		assertFalse(RotaConstraintProvider.isGenderMismatch(carer(Gender.FEMALE), null));
		assertFalse(RotaConstraintProvider.isGenderMismatch(carer(Gender.MALE), null));
	}

	@Test
	void anyIsNotAMismatch() {
		assertFalse(RotaConstraintProvider.isGenderMismatch(carer(Gender.MALE), Gender.ANY));
	}

	@Test
	void wrongGenderIsAMismatchAndRightGenderIsNot() {
		assertTrue(RotaConstraintProvider.isGenderMismatch(carer(Gender.MALE), Gender.FEMALE));
		assertFalse(RotaConstraintProvider.isGenderMismatch(carer(Gender.FEMALE), Gender.FEMALE));
	}

	@Test
	void unassignedSlotIsNotAMismatch() {
		assertFalse(RotaConstraintProvider.isGenderMismatch(null, Gender.FEMALE));
	}

	@Test
	void templateNeverStoresABlankGender() {
		ShiftTemplate t = new ShiftTemplate();
		t.setRequiredGender(null);
		assertEquals(Gender.ANY, t.getRequiredGender());
		t.setGender(null);
		assertEquals(Gender.ANY, t.getGender());
		t.setRequiredGender(Gender.FEMALE);
		assertEquals(Gender.FEMALE, t.getRequiredGender());
	}
}
