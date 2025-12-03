package staffschedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.midco.rota.ShiftTypeLimitConfig;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;

/**
 * Diagnostic utility to find why specific shifts cannot be assigned to employees
 */
public class ShiftAssignmentDiagnostics {

    public static void diagnoseUnassignedShifts(List<ShiftAssignment> assignments, List<Employee> employees) {
        System.out.println("\n========== SHIFT ASSIGNMENT DIAGNOSTICS ==========\n");
        
        // Find unassigned shifts
        List<ShiftAssignment> unassigned = assignments.stream()
            .filter(sa -> sa.getEmployee() == null)
            .collect(Collectors.toList());
        
        System.out.println("Total Shifts: " + assignments.size());
        System.out.println("Assigned: " + (assignments.size() - unassigned.size()));
        System.out.println("Unassigned: " + unassigned.size());
        
        // Group by shift type
        Map<ShiftType, Long> unassignedByType = unassigned.stream()
            .collect(Collectors.groupingBy(
                sa -> sa.getShift().getShiftTemplate().getShiftType(),
                Collectors.counting()
            ));
        
        System.out.println("\nUnassigned shifts by type:");
        unassignedByType.forEach((type, count) -> 
            System.out.println("  " + type + ": " + count)
        );
        
        // Analyze each unassigned DAY shift
        List<ShiftAssignment> unassignedDay = unassigned.stream()
            .filter(sa -> sa.getShift().getShiftTemplate().getShiftType() == ShiftType.DAY)
            .collect(Collectors.toList());
        
        if (!unassignedDay.isEmpty()) {
            System.out.println("\n========== ANALYZING DAY SHIFTS ==========\n");
            
            for (ShiftAssignment sa : unassignedDay.stream().limit(3).collect(Collectors.toList())) {
                analyzeShiftAssignment(sa, employees);
            }
        }
    }
    
    private static void analyzeShiftAssignment(ShiftAssignment sa, List<Employee> employees) {
        Shift shift = sa.getShift();
        String location = shift.getShiftTemplate().getLocation();
        ShiftType shiftType = shift.getShiftTemplate().getShiftType();
        LocalDate date = shift.getShiftStart();
        Gender requiredGender = shift.getShiftTemplate().getGender();
        
        System.out.println("─────────────────────────────────────────────────");
        System.out.println("Shift: " + location + " | " + shiftType + " | " + date);
        System.out.println("Required Gender: " + requiredGender);
        System.out.println("─────────────────────────────────────────────────");
        
        List<String> blockedReasons = new ArrayList<>();
        int eligibleCount = 0;
        
        for (Employee emp : employees) {
            List<String> reasons = checkEmployeeEligibility(emp, shift);
            
            if (reasons.isEmpty()) {
                eligibleCount++;
            } else if (eligibleCount < 3) { // Show first 3 blocked employees
                System.out.println("\nEmployee: " + emp.getName() + " (ID: " + emp.getId() + ")");
                System.out.println("  BLOCKED by:");
                reasons.forEach(r -> System.out.println("    ✗ " + r));
            }
        }
        
        System.out.println("\n✓ Eligible employees: " + eligibleCount + " / " + employees.size());
        System.out.println();
    }
    
    private static List<String> checkEmployeeEligibility(Employee emp, Shift shift) {
        List<String> reasons = new ArrayList<>();
        
        String location = shift.getShiftTemplate().getLocation();
        ShiftType shiftType = shift.getShiftTemplate().getShiftType();
        LocalDate date = shift.getShiftStart();
        Gender requiredGender = shift.getShiftTemplate().getGender();
        
        // Check gender
        if (requiredGender != Gender.ANY && emp.getGender() != requiredGender) {
            reasons.add("Gender mismatch (required: " + requiredGender + ", has: " + emp.getGender() + ")");
        }
        
        // Check restricted day
        if (emp.getRestrictedDay() != null && 
            emp.getRestrictedDay().contains(date.getDayOfWeek())) {
            reasons.add("Restricted day: " + date.getDayOfWeek());
        }
        
        // Check restricted shift type
        if (emp.getRestrictedShift() != null && 
            emp.getRestrictedShift().contains(shiftType)) {
            reasons.add("Restricted shift type: " + shiftType);
        }
        
        // Check restricted service
        if (emp.getRestrictedService() != null && 
            emp.getRestrictedService().contains(location)) {
            reasons.add("Restricted location: " + location);
        }
        
        // Check schedule pattern
        if (!emp.canWorkShift(location, date, shiftType)) {
            reasons.add("Schedule pattern violation");
        }
        
        // Check max hours per shift type per day
        Map<ShiftType, Integer> maxHoursPerShiftType = ShiftTypeLimitConfig.maxHoursPerShiftType();
        if (maxHoursPerShiftType.containsKey(shiftType)) {
            int maxMinutes = maxHoursPerShiftType.get(shiftType) * 60;
            long shiftMinutes = shift.getDurationInMins();
            if (shiftMinutes > maxMinutes) {
                reasons.add("Shift duration (" + (shiftMinutes/60) + "h) exceeds max for type (" + 
                    (maxMinutes/60) + "h)");
            }
        }
        
        // Check weekly shift type limit
        Map<ShiftType, Integer> weeklyLimit = ShiftTypeLimitConfig.weeklyShiftTypeLimit();
        if (weeklyLimit.containsKey(shiftType)) {
            reasons.add("Note: Weekly " + shiftType + " limit is " + weeklyLimit.get(shiftType));
        }
        
        return reasons;
    }
    
    /**
     * Call this method in your test AFTER solving to see what blocked assignments
     */
    public static void diagnosePostSolve(List<ShiftAssignment> assignments, List<Employee> employees) {
        System.out.println("\n========== POST-SOLVE DIAGNOSTICS ==========\n");
        
        // Find shifts that have diagnostic reasons
        List<ShiftAssignment> withReasons = assignments.stream()
            .filter(sa -> sa.getDiagnosticReasons() != null && !sa.getDiagnosticReasons().isEmpty())
            .collect(Collectors.toList());
        
        if (withReasons.isEmpty()) {
            System.out.println("No diagnostic information available.");
            System.out.println("Consider enabling constraint logging in OptaPlanner.");
            return;
        }
        
        // Group by shift type
        Map<ShiftType, List<ShiftAssignment>> byType = withReasons.stream()
            .collect(Collectors.groupingBy(sa -> sa.getShift().getShiftTemplate().getShiftType()));
        
        byType.forEach((type, list) -> {
            System.out.println("\n" + type + " shifts with issues: " + list.size());
            list.stream().limit(5).forEach(sa -> {
                System.out.println("  " + sa.getShift().getShiftTemplate().getLocation() + 
                    " " + sa.getShift().getShiftStart());
                sa.getDiagnosticReasons().forEach(r -> System.out.println("    - " + r));
            });
        });
    }
}