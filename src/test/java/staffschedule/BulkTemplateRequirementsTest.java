package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.midco.rota.controller.ShiftTemplateController;
import com.midco.rota.dto.ShiftTemplateBulkRequest;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.ShiftTypeDef;
import com.midco.rota.opt.ShiftTypeMeta;
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.util.Gender;

/**
 * Bulk Create (the template create screen) sets gender and required skills per shift-type
 * row; the type's own eligibility skill (e.g. SHIFT_LEAD) is always kept.
 */
class BulkTemplateRequirementsTest {

	private final ShiftTemplateRepository repo = mock(ShiftTemplateRepository.class);
	private final ShiftTemplateController controller = new ShiftTemplateController(repo);

	@BeforeEach
	void types() {
		ShiftTypeDef day = new ShiftTypeDef();
		day.setCode("DAY");
		day.setActive(true);
		ShiftTypeDef lead = new ShiftTypeDef();
		lead.setCode("SHIFT_LEAD");
		lead.setActive(true);
		lead.setRequiredSkill("SHIFT_LEAD");
		ShiftTypeMeta.load(List.of(day, lead));
	}

	@AfterEach
	void reset() {
		ShiftTypeMeta.load(List.of());
	}

	private ShiftTemplateBulkRequest.Row row(String type, Gender g, List<String> skills) {
		ShiftTemplateBulkRequest.Row r = new ShiftTemplateBulkRequest.Row();
		r.setShiftType(type);
		r.setDays(List.of(DayOfWeek.MONDAY));
		r.setStartTime(LocalTime.of(8, 0));
		r.setEndTime(LocalTime.of(20, 0));
		r.setEmpCount(1);
		r.setRequiredGender(g);
		r.setRequiredSkills(skills);
		return r;
	}

	@SuppressWarnings("unchecked")
	private List<ShiftTemplate> save(ShiftTemplateBulkRequest.Row... rows) {
		when(repo.findByLocation("House")).thenReturn(new ArrayList<>());
		ShiftTemplateBulkRequest req = new ShiftTemplateBulkRequest();
		req.setLocation("House");
		req.setRegion("REGION");
		req.setRows(List.of(rows));
		controller.bulkSetTemplates(req);
		ArgumentCaptor<List<ShiftTemplate>> saved = ArgumentCaptor.forClass(List.class);
		verify(repo).saveAll(saved.capture());
		return saved.getValue();
	}

	@Test
	void genderAndSkillsFromTheGridAreStored() {
		ShiftTemplate t = save(row("DAY", Gender.MALE, List.of("Carer", "BUCCAL"))).get(0);
		assertEquals(Gender.MALE, t.getRequiredGender());
		assertEquals(List.of("Carer", "BUCCAL"), t.getRequiredSkills());
	}

	@Test
	void theTypesOwnSkillIsAlwaysKept() {
		ShiftTemplate t = save(row("SHIFT_LEAD", Gender.ANY, List.of("Carer"))).get(0);
		assertTrue(t.getRequiredSkills().contains("SHIFT_LEAD"), t.getRequiredSkills().toString());
		assertTrue(t.getRequiredSkills().contains("Carer"));
	}

	@Test
	void noGenderSentMeansAny() {
		ShiftTemplate t = save(row("DAY", null, List.of())).get(0);
		assertEquals(Gender.ANY, t.getRequiredGender());
	}
}
