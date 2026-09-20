package com.midco.rota.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;

@Service
public class ConstraintExplanationService {


	private final ScoreManager<Rota, ?> scoreManager;

    public ConstraintExplanationService(ScoreManager<Rota, ?> scoreManager) {
        this.scoreManager = scoreManager;
    }


    public List<ConstraintMatchTotal<?>> getConstraintViolations(Rota rota) {
        ScoreExplanation<Rota, ?> explanation = scoreManager.explainScore(rota);
        return List.copyOf(explanation.getConstraintMatchTotalMap().values());
    }

    /**
     * Full score breakdown for a (usually persisted) rota: the score, the
     * per-constraint totals (hard/soft/matches, worst hard first), and every slot
     * that carries a hard violation with the exact constraints it breaks. The caller
     * must have rehydrated the rota's transient planning state (constraint config,
     * per-employee unavailable dates, SLEEP_IN pairing, planning ids) first, or the
     * score will not match what the solver produced.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, Object> explainDetailed(Rota rota) {
        ScoreExplanation<Rota, ?> explanation = scoreManager.explainScore(rota);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("score", explanation.getScore().toString());

        // Per-constraint totals, most-negative hard first.
        List<Map<String, Object>> constraints = new ArrayList<>();
        for (ConstraintMatchTotal<?> cmt : explanation.getConstraintMatchTotalMap().values()) {
            HardSoftLongScore s = (HardSoftLongScore) cmt.getScore();
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("constraint", cmt.getConstraintName());
            c.put("hard", s.hardScore());
            c.put("soft", s.softScore());
            c.put("matches", cmt.getConstraintMatchCount());
            constraints.add(c);
        }
        constraints.sort(Comparator.comparingLong(c -> ((Number) c.get("hard")).longValue()));
        out.put("constraints", constraints);

        // Per-slot indictments, only slots that carry a hard violation.
        Map<Object, Indictment> indictments = (Map) explanation.getIndictmentMap();
        List<Map<String, Object>> slots = new ArrayList<>();
        for (ShiftAssignment sa : rota.getShiftAssignmentList()) {
            Indictment ind = indictments.get(sa);
            if (ind == null) {
                continue;
            }
            HardSoftLongScore s = (HardSoftLongScore) ind.getScore();
            if (s.hardScore() >= 0L) {
                continue; // not a hard violation
            }
            Set<String> broken = new TreeSet<>();
            for (Object cmObj : ind.getConstraintMatchSet()) {
                ConstraintMatch cm = (ConstraintMatch) cmObj;
                if (((HardSoftLongScore) cm.getScore()).hardScore() < 0L) {
                    broken.add(cm.getConstraintName());
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assignmentId", sa.getId());
            m.put("date", sa.getShift() != null ? sa.getShift().getShiftStart() : null);
            ShiftTemplate st = (sa.getShift() != null) ? sa.getShift().getShiftTemplate() : null;
            m.put("location", st != null ? st.getLocation() : null);
            m.put("shiftType", st != null ? st.getShiftTypeCode() : null);
            m.put("employee", sa.getEmployee() != null ? sa.getEmployee().getName() : null);
            m.put("employeeId", sa.getEmployee() != null ? sa.getEmployee().getId() : null);
            m.put("pinned", sa.isPinned());
            m.put("hard", s.hardScore());
            m.put("brokenConstraints", new ArrayList<>(broken));
            slots.add(m);
        }
        slots.sort(Comparator.comparingLong(m -> ((Number) m.get("hard")).longValue()));
        out.put("violatingSlotCount", slots.size());
        out.put("violatingSlots", slots);
        return out;
    }
}
