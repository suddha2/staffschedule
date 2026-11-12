package com.midco.rota.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.SolverTuningSetting;
import com.midco.rota.opt.ShiftTypeMeta;
import com.midco.rota.repository.SolverTuningSettingRepository;

/**
 * Checks a shift template must pass before it is saved as active (V045). A template that
 * fails is refused, so a shift the solver could never fill, or one with no statutory break,
 * can't reach a rota:
 * <ul>
 * <li>start and end set and different; the break shorter than the shift;</li>
 * <li>statutory break: a working shift over {@code statutoryBreakAfterMinutes} after its break
 * needs at least {@code statutoryBreakMinutes} of break (solver_tuning, 360 and 20);</li>
 * <li>worked hours within the shift type's daily limit ({@code shift_type.max_hours_per_day}),
 * which the solver enforces as a hard rule, so a longer template would never be filled.</li>
 * </ul>
 * Inactive templates are not checked, so a broken template can always be switched off.
 */
@Service
public class ShiftTemplateRules {

	static final int DEFAULT_BREAK_AFTER_MINUTES = 360;
	static final int DEFAULT_BREAK_MINUTES = 20;

	private final SolverTuningSettingRepository tuning;

	public ShiftTemplateRules(SolverTuningSettingRepository tuning) {
		this.tuning = tuning;
	}

	/** Problems with one template, in plain words; empty when it can be saved. */
	public List<String> problems(ShiftTemplate t) {
		return problems(t, setting("statutoryBreakAfterMinutes", DEFAULT_BREAK_AFTER_MINUTES),
				setting("statutoryBreakMinutes", DEFAULT_BREAK_MINUTES));
	}

	/**
	 * Problems across several templates, each message once, prefixed with the template's
	 * day (and type, when the list mixes types) so a multi-day save says where to look.
	 */
	public List<String> problems(Collection<ShiftTemplate> templates) {
		int after = setting("statutoryBreakAfterMinutes", DEFAULT_BREAK_AFTER_MINUTES);
		int min = setting("statutoryBreakMinutes", DEFAULT_BREAK_MINUTES);
		boolean mixedTypes = templates.stream().map(ShiftTemplate::getShiftTypeCode).distinct().count() > 1;
		Map<String, List<String>> where = new LinkedHashMap<>();
		for (ShiftTemplate t : templates) {
			if (!t.isActive()) {
				continue;
			}
			for (String p : problems(t, after, min)) {
				String key = (mixedTypes ? label(t.getShiftTypeCode()) + ": " : "") + p;
				where.computeIfAbsent(key, k -> new ArrayList<>())
						.add(t.getDayOfWeek() == null ? "" : day(t.getDayOfWeek().name()));
			}
		}
		List<String> out = new ArrayList<>();
		where.forEach((msg, days) -> {
			List<String> named = days.stream().filter(d -> !d.isEmpty()).distinct().toList();
			out.add(named.isEmpty() || templates.size() == 1 ? msg : String.join(", ", named) + ": " + msg);
		});
		return out;
	}

	static List<String> problems(ShiftTemplate t, int breakAfterMinutes, int minBreakMinutes) {
		List<String> out = new ArrayList<>();
		if (!t.isActive()) {
			return out;
		}
		if (t.getStartTime() == null || t.getEndTime() == null) {
			out.add("Start and end times are required.");
			return out;
		}
		if (t.getStartTime().equals(t.getEndTime())) {
			out.add("Start and end are the same time.");
			return out;
		}
		long span = t.getSpanMinutes();
		int brk = t.getBreakMinutes();
		if (brk < 0) {
			out.add("The break can't be negative.");
			return out;
		}
		if (brk >= span) {
			out.add("The break (" + hm(brk) + ") is as long as the shift (" + hm(span) + ").");
			return out;
		}
		long worked = span - brk;
		String code = t.getShiftTypeCode();
		if (code != null && ShiftTypeMeta.countsAsWork(code) && worked > breakAfterMinutes && brk < minBreakMinutes) {
			out.add("A " + hm(span) + " shift needs a break of at least " + hm(minBreakMinutes)
					+ " (statutory break for more than " + hm(breakAfterMinutes) + " worked)"
					+ (brk == 0 ? "." : "; " + hm(brk) + " is entered."));
		}
		Integer maxHours = code == null ? null : ShiftTypeMeta.maxHoursPerDay(code);
		if (maxHours != null && worked > maxHours * 60L) {
			out.add(hm(worked) + " worked is over the " + maxHours + " h daily limit for " + label(code)
					+ ", so no carer could be given this shift. Shorten it or lengthen the break.");
		}
		return out;
	}

	private int setting(String key, int fallback) {
		return tuning.findById(key).map(SolverTuningSetting::getIntValue).orElse(fallback);
	}

	/** 780 -> "13 h", 20 -> "20 min", 750 -> "12 h 30 min". */
	static String hm(long minutes) {
		long h = minutes / 60, m = minutes % 60;
		if (h == 0) {
			return m + " min";
		}
		return m == 0 ? h + " h" : h + " h " + m + " min";
	}

	private static String label(String code) {
		return code == null ? "this shift type" : code.replace('_', ' ').toLowerCase();
	}

	private static String day(String name) {
		return name.charAt(0) + name.substring(1).toLowerCase();
	}
}
