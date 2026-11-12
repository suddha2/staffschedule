package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.midco.rota.controller.ShiftTemplateController;
import com.midco.rota.dto.ShiftTemplateBulkRequest;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.ShiftTypeDef;
import com.midco.rota.model.SolverTuningSetting;
import com.midco.rota.opt.ShiftTypeMeta;
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.repository.SolverTuningSettingRepository;
import com.midco.rota.service.ShiftTemplateRules;

/** V045: breaks are minutes, and a template that fails the save checks is refused. */
class ShiftTemplateRulesTest {

	private final SolverTuningSettingRepository tuning = mock(SolverTuningSettingRepository.class);
	private final ShiftTemplateRules rules = new ShiftTemplateRules(tuning);

	@BeforeEach
	void types() {
		ShiftTypeMeta.load(List.of(type("WAKING_NIGHT", true, 12), type("FLOATING", true, 6),
				type("LONG_DAY", true, 15), type("SLEEP_IN", false, null)));
	}

	@AfterEach
	void reset() {
		ShiftTypeMeta.load(List.of());
	}

	private static ShiftTypeDef type(String code, boolean work, Integer maxHours) {
		ShiftTypeDef d = new ShiftTypeDef();
		d.setCode(code);
		d.setActive(true);
		d.setCountsAsWork(work);
		d.setMaxHoursPerDay(maxHours);
		return d;
	}

	private static ShiftTemplate t(String code, String start, String end, int breakMins) {
		ShiftTemplate t = new ShiftTemplate();
		t.setShiftTypeCode(code);
		t.setDayOfWeek(DayOfWeek.MONDAY);
		t.setStartTime(LocalTime.parse(start));
		t.setEndTime(LocalTime.parse(end));
		t.setBreakMinutes(breakMins);
		t.setActive(true);
		return t;
	}

	@Test
	void alexandraFlatBWakingNightWithNoBreakIsRefused() {
		List<String> p = rules.problems(t("WAKING_NIGHT", "20:00", "09:00", 0));
		assertEquals(2, p.size(), p.toString());
		assertTrue(p.get(0).contains("needs a break of at least 20 min"), p.get(0));
		assertTrue(p.get(1).contains("13 h worked is over the 12 h daily limit"), p.get(1));
	}

	@Test
	void anHourBreakBringsItToTwelveHoursAndIsAccepted() {
		assertEquals(List.of(), rules.problems(t("WAKING_NIGHT", "20:00", "09:00", 60)));
	}

	@Test
	void goldersRiseEightHourFloatingIsOverTheLimitEvenWithABreak() {
		List<String> p = rules.problems(t("FLOATING", "12:00", "20:00", 30));
		assertEquals(1, p.size(), p.toString());
		assertTrue(p.get(0).contains("7 h 30 min worked is over the 6 h daily limit for floating"), p.get(0));
	}

	@Test
	void shortShiftsAndSleepInsNeedNoBreak() {
		assertEquals(List.of(), rules.problems(t("FLOATING", "16:00", "20:00", 0)));
		assertEquals(List.of(), rules.problems(t("SLEEP_IN", "22:00", "07:00", 0)));
	}

	@Test
	void aBreakUnderTwentyMinutesOnALongShiftIsRefused() {
		List<String> p = rules.problems(t("LONG_DAY", "07:00", "22:00", 15));
		assertEquals(1, p.size(), p.toString());
		assertTrue(p.get(0).contains("15 min is entered"), p.get(0));
	}

	@Test
	void badTimesAndBreaksAreRefused() {
		assertTrue(rules.problems(t("LONG_DAY", "07:00", "07:00", 0)).get(0).contains("same time"));
		assertTrue(rules.problems(t("FLOATING", "16:00", "17:00", 60)).get(0).contains("as long as the shift"));
	}

	@Test
	void inactiveTemplatesAreNotChecked() {
		ShiftTemplate off = t("WAKING_NIGHT", "20:00", "09:00", 0);
		off.setActive(false);
		assertEquals(List.of(), rules.problems(off));
	}

	@Test
	void thresholdsComeFromSolverTuning() {
		SolverTuningSetting min = mock(SolverTuningSetting.class);
		when(min.getIntValue()).thenReturn(60);
		when(tuning.findById("statutoryBreakMinutes")).thenReturn(Optional.of(min));
		when(tuning.findById("statutoryBreakAfterMinutes")).thenReturn(Optional.empty());
		assertTrue(rules.problems(t("LONG_DAY", "07:00", "22:00", 30)).get(0).contains("at least 1 h"));
	}

	@Test
	void workedHoursAreTheSpanLessTheBreak() {
		ShiftTemplate wn = t("WAKING_NIGHT", "20:00", "09:00", 60);
		assertEquals(780, wn.getSpanMinutes());
		assertEquals(720, wn.getWorkedMinutes());
		Shift s = new Shift();
		s.setShiftTemplate(wn);
		s.setShiftStart(LocalDate.of(2026, 12, 7));
		s.setShiftEnd(LocalDate.of(2026, 12, 8));
		assertEquals(720, s.getDurationInMins());
		assertEquals("12.00", s.getDurationInHours().toPlainString());
	}

	@Test
	void multiDaySavesNameTheDays() {
		ShiftTemplate mon = t("WAKING_NIGHT", "20:00", "09:00", 60);
		ShiftTemplate tue = t("WAKING_NIGHT", "20:00", "09:00", 0);
		tue.setDayOfWeek(DayOfWeek.TUESDAY);
		List<String> p = rules.problems(List.of(mon, tue));
		assertTrue(p.get(0).startsWith("Tuesday: "), p.toString());
	}

	@Test
	@SuppressWarnings("unchecked")
	void theGridRefusesAndSavesNothing() {
		ShiftTemplateRepository repo = mock(ShiftTemplateRepository.class);
		when(repo.findByLocation("Alexandra Flat B - LC")).thenReturn(new ArrayList<>());
		ShiftTemplateController controller = new ShiftTemplateController(repo, rules);
		ShiftTemplateBulkRequest.Row row = new ShiftTemplateBulkRequest.Row();
		row.setShiftType("WAKING_NIGHT");
		row.setDays(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
		row.setStartTime(LocalTime.of(20, 0));
		row.setEndTime(LocalTime.of(9, 0));
		ShiftTemplateBulkRequest req = new ShiftTemplateBulkRequest();
		req.setLocation("Alexandra Flat B - LC");
		req.setRegion("BARNET");
		req.setRows(List.of(row));
		ResponseEntity<?> r = controller.bulkSetTemplates(req);
		assertEquals(400, r.getStatusCode().value());
		List<String> problems = (List<String>) ((Map<String, Object>) r.getBody()).get("problems");
		assertTrue(problems.get(0).contains("Monday") && problems.get(0).contains("Tuesday"), problems.toString());
		verify(repo, never()).saveAll(any());
	}
}
