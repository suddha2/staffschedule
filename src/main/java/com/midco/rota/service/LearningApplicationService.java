package com.midco.rota.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Learning;
import com.midco.rota.model.Learning.LearningType;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.LearningRepository;
import com.midco.rota.util.ShiftType;

/**
 * Service to apply learned patterns to Employee data Works with Employee's
 * List<String> preferredService format: ["LOCATION:60", "LOCATION2:40"]
 */
@Service
public class LearningApplicationService {

	@Autowired
	private LearningRepository learningRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	// Auto-apply threshold (80% confidence or higher)
	private static final double AUTO_APPLY_THRESHOLD = 0.80;

	/**
	 * Get all pending (unapplied) learnings
	 */
	public List<Learning> getPendingLearnings() {
		return learningRepository.findByAppliedFalse();
	}

	/**
	 * Get high confidence learnings (>= 80%) ready for auto-apply
	 */
	public List<Learning> getHighConfidenceLearnings() {
		return learningRepository.findByAppliedFalseAndConfidenceGreaterThanEqual(AUTO_APPLY_THRESHOLD);
	}

	/**
	 * Apply a specific learning by ID
	 */
	@Transactional
	public boolean applyLearning(Long learningId) {
		Learning learning = learningRepository.findById(learningId).orElse(null);
		if (learning == null) {
			System.out.println("❌ Learning not found: " + learningId);
			return false;
		}

		if (learning.getApplied()) {
			System.out.println("⚠️  Learning already applied: " + learningId);
			return false;
		}

		boolean success = applyLearningToEmployee(learning);

		if (success) {
			learning.setApplied(true);
			learning.setAppliedDate(LocalDateTime.now());
			learningRepository.save(learning);
			System.out.println("✓ Applied learning: " + learning.getDescription());
		} else {
			System.out.println("❌ Failed to apply learning: " + learning.getDescription());
		}

		return success;
	}

	/**
	 * Auto-apply all high confidence learnings (>= 80%)
	 */
	@Transactional
	public int autoApplyHighConfidenceLearnings() {
		List<Learning> highConfidence = getHighConfidenceLearnings();

		System.out.println("\n=== AUTO-APPLYING HIGH CONFIDENCE LEARNINGS ===");
		System.out.println("Found " + highConfidence.size() + " learnings with confidence >= 80%");

		int appliedCount = 0;
		for (Learning learning : highConfidence) {
			if (applyLearning(learning.getId())) {
				appliedCount++;
			}
		}

		System.out.println("=== AUTO-APPLY COMPLETE: " + appliedCount + " learnings applied ===\n");
		return appliedCount;
	}

	/**
	 * Reject a learning (mark as reviewed but not applied)
	 */
	@Transactional
	public void rejectLearning(Long learningId, String reason) {
		Learning learning = learningRepository.findById(learningId).orElse(null);
		if (learning != null) {
			learning.setApplied(true); // Mark as reviewed
			learning.setAppliedDate(LocalDateTime.now());

			String originalDescription = learning.getDescription();
			learning.setDescription(originalDescription + " | REJECTED: " + reason);
			learningRepository.save(learning);
			System.out.println("⚠️  Rejected learning: " + learning.getDescription() + " | Reason: " + reason);
		}
	}

	/**
	 * Apply learning to employee based on type
	 */
	private boolean applyLearningToEmployee(Learning learning) {
		try {
			switch (learning.getType()) {
			case EMPLOYEE_PREFERENCE:
				return applyLocationPreference(learning);

			case DAY_PREFERENCE:
				return applyDayPreference(learning);

			case SHIFT_TYPE_PREFERENCE:
				return applyShiftTypePreference(learning);

			case CONSTRAINT_WEIGHT:
				return logConstraintWeightRecommendation(learning);

			case NEW_PATTERN:
				return logNewPattern(learning);

			default:
				System.out.println("⚠️  Unknown learning type: " + learning.getType());
				return false;
			}
		} catch (Exception e) {
			System.out.println("❌ Error applying learning: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Apply location preference learning Updates Employee's List<String>
	 * preferredService with format "LOCATION:WEIGHTAGE"
	 */
	private boolean applyLocationPreference(Learning learning) {
		Employee employee = employeeRepository.findById(learning.getEmployeeId()).orElse(null);
		if (employee == null) {
			System.out.println("❌ Employee not found: " + learning.getEmployeeId());
			return false;
		}

		// Parse new value: "LOCATION:90"
		String[] parts = learning.getNewValue().split(":");
		if (parts.length != 2) {
			System.out.println("❌ Invalid location preference format: " + learning.getNewValue());
			return false;
		}

		String location = parts[0].trim();
		int weightage = Integer.parseInt(parts[1].trim());

		// Get current preferredService list
		List<String> preferredService = employee.getPreferredService();
		if (preferredService == null) {
			preferredService = new ArrayList<>();
		} else {
			// Make mutable copy
			preferredService = new ArrayList<>(preferredService);
		}

		// Update or add location preference
		updateLocationInList(preferredService, location, weightage);

		// Save back to employee
		employee.setPreferredService(preferredService);
		employeeRepository.save(employee);

		System.out.println(String.format("  ✓ Updated %s %s location preference: %s → %d%%", employee.getFirstName(),
				employee.getLastName(), location, weightage));

		return true;
	}

	/**
	 * Update location weightage in preferredService list Format: ["LOCATION1:60",
	 * "LOCATION2:40", ...]
	 */
	private void updateLocationInList(List<String> preferredService, String location, int weightage) {
		// Find and remove existing entry for this location
		preferredService.removeIf(entry -> {
			String loc = entry.contains(":") ? entry.split(":")[0].trim() : entry.trim();
			return loc.equalsIgnoreCase(location);
		});

		// Add new entry
		preferredService.add(location + ":" + weightage);

		// Sort by weightage descending (optional but nice)
		preferredService.sort((a, b) -> {
			int weightA = getWeightageFromEntry(a);
			int weightB = getWeightageFromEntry(b);
			return Integer.compare(weightB, weightA); // Descending
		});
	}

	/**
	 * Extract weightage from "LOCATION:60" format
	 */
	private int getWeightageFromEntry(String entry) {
		if (entry.contains(":")) {
			try {
				return Integer.parseInt(entry.split(":")[1].trim());
			} catch (Exception e) {
				return 0;
			}
		}
		return 100; // Old format without weight defaults to 100
	}

	/**
	 * Apply day preference learning Adds DayOfWeek to Employee's List<DayOfWeek>
	 * preferredDays
	 */
	private boolean applyDayPreference(Learning learning) {
		Employee employee = employeeRepository.findById(learning.getEmployeeId()).orElse(null);
		if (employee == null) {
			System.out.println("❌ Employee not found: " + learning.getEmployeeId());
			return false;
		}

		// Parse day from parameter
		DayOfWeek day;
		try {
			day = DayOfWeek.valueOf(learning.getParameter().toUpperCase());
		} catch (Exception e) {
			System.out.println("❌ Invalid day: " + learning.getParameter());
			return false;
		}

		// Get current preferredDays list
		List<DayOfWeek> preferredDays = employee.getPreferredDays();
		if (preferredDays == null) {
			preferredDays = new ArrayList<>();
		} else {
			preferredDays = new ArrayList<>(preferredDays); // Make mutable copy
		}

		// Add if not already present
		if (!preferredDays.contains(day)) {
			preferredDays.add(day);
			employee.setPreferredDays(preferredDays);
			employeeRepository.save(employee);

			System.out.println(String.format("  ✓ Updated %s %s day preference: added %s", employee.getFirstName(),
					employee.getLastName(), day));
			return true;
		} else {
			System.out.println(String.format("  ⚠️  %s %s already has %s in preferred days", employee.getFirstName(),
					employee.getLastName(), day));
			return true; // Not an error, just already set
		}
	}

	/**
	 * Apply shift type preference learning Adds ShiftType to Employee's
	 * List<ShiftType> preferredShifts
	 */
	private boolean applyShiftTypePreference(Learning learning) {
		Employee employee = employeeRepository.findById(learning.getEmployeeId()).orElse(null);
		if (employee == null) {
			System.out.println("❌ Employee not found: " + learning.getEmployeeId());
			return false;
		}

		// Parse shift type from parameter
		ShiftType shiftType;
		try {
			shiftType = ShiftType.valueOf(learning.getParameter().toUpperCase());
		} catch (Exception e) {
			System.out.println("❌ Invalid shift type: " + learning.getParameter());
			return false;
		}

		// Get current preferredShifts list
		List<ShiftType> preferredShifts = employee.getPreferredShifts();
		if (preferredShifts == null) {
			preferredShifts = new ArrayList<>();
		} else {
			preferredShifts = new ArrayList<>(preferredShifts); // Make mutable copy
		}

		// Add if not already present
		if (!preferredShifts.contains(shiftType)) {
			preferredShifts.add(shiftType);
			employee.setPreferredShifts(preferredShifts);
			employeeRepository.save(employee);

			System.out.println(String.format("  ✓ Updated %s %s shift type preference: added %s",
					employee.getFirstName(), employee.getLastName(), shiftType));
			return true;
		} else {
			System.out.println(String.format("  ⚠️  %s %s already has %s in preferred shifts", employee.getFirstName(),
					employee.getLastName(), shiftType));
			return true; // Not an error, just already set
		}
	}

	/**
	 * Log constraint weight recommendation (requires manual code change)
	 */
	private boolean logConstraintWeightRecommendation(Learning learning) {
		System.out.println("\n=== CONSTRAINT WEIGHT RECOMMENDATION ===");
		System.out.println(learning.getDescription());
		System.out.println(
				"⚠️  Manual action required: Update " + learning.getParameter() + " in RotaConstraintProvider.java");
		System.out.println("   Old value: " + learning.getOldValue());
		System.out.println("   Suggested: " + learning.getNewValue());
		System.out.println("==========================================\n");
		return true; // Logged successfully
	}

	/**
	 * Log new pattern for manual review
	 */
	private boolean logNewPattern(Learning learning) {
		System.out.println("\n=== NEW PATTERN DETECTED ===");
		System.out.println(learning.getDescription());
		System.out.println("⚠️  Manual review required for: " + learning.getParameter());
		System.out.println("============================\n");
		return true; // Logged successfully
	}

	/**
	 * Get statistics about learnings
	 */
	public Map<String, Object> getLearningStats() {
		Map<String, Object> stats = new HashMap<>();

		long totalLearnings = learningRepository.count();
		long appliedLearnings = learningRepository.findByAppliedTrue().size();
		long pendingLearnings = learningRepository.findByAppliedFalse().size();

		stats.put("total", totalLearnings);
		stats.put("applied", appliedLearnings);
		stats.put("pending", pendingLearnings);

		// Count by type
		List<Object[]> byType = learningRepository.countPendingByType();
		Map<String, Long> typeMap = byType.stream()
				.collect(Collectors.toMap(arr -> arr[0].toString(), arr -> (Long) arr[1]));
		stats.put("byType", typeMap);

		return stats;
	}
}