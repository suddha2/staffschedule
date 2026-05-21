package com.midco.rota.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.service.EmployeeAccessService;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    
    private final EmployeeRepository employeeRepository;
    private final EmployeeAccessService employeeAccessService;

    public EmployeeController(EmployeeRepository employeeRepository,
            EmployeeAccessService employeeAccessService) {
        this.employeeRepository = employeeRepository;
        this.employeeAccessService = employeeAccessService;
    }
    
    // GET all employees
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return ResponseEntity.ok(employees);
    }
    
    // GET employee by ID
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Integer id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Employee> toggleActive(@PathVariable Integer id) {
        return employeeRepository.findById(id)
                .map(template -> {
                    template.setActive(!template.isActive());
                    Employee updated = employeeRepository.save(template);
                    // Switched OFF -> revoke mobile access: drop device
                    // registrations and any outstanding login codes. Their
                    // PASETO is rejected on next request by the auth filter.
                    if (!updated.isActive()) {
                        employeeAccessService.revokeMobileAccess(updated.getId(), updated.getEmail());
                    }
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // POST - Create new employee
    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {
        try {
            employee.setId(null);
            Employee savedEmployee = employeeRepository.save(employee);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "An employee with that email already exists."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // PUT - Update existing employee
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable Integer id,
            @RequestBody Employee employeeDetails) {

        Optional<Employee> existing = employeeRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Employee employee = existing.get();

        // Basic fields
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        employee.setEmail(employeeDetails.getEmail());
        employee.setGender(employeeDetails.getGender());
        employee.setContractType(employeeDetails.getContractType());
        employee.setMinHrs(employeeDetails.getMinHrs());
        employee.setMaxHrs(employeeDetails.getMaxHrs());
        employee.setRateCode(employeeDetails.getRateCode());
        employee.setRestDays(employeeDetails.getRestDays());

        // Region and Services
        employee.setPreferredRegion(employeeDetails.getPreferredRegion());
        // preferredService stores "ServiceName:Weight" format
        employee.setPreferredService(employeeDetails.getPreferredService());
        employee.setRestrictedService(employeeDetails.getRestrictedService());

        // Days and Shifts
        employee.setPreferredDays(employeeDetails.getPreferredDays());
        employee.setRestrictedDays(employeeDetails.getRestrictedDays());
        employee.setPreferredShifts(employeeDetails.getPreferredShifts());
        employee.setRestrictedShifts(employeeDetails.getRestrictedShifts());

        // Skills and Pattern
        employee.setSkills(employeeDetails.getSkills());
        employee.setDaysOn(employeeDetails.getDaysOn());
        employee.setDaysOff(employeeDetails.getDaysOff());
        employee.setWeekOn(employeeDetails.getWeekOn());
        employee.setWeekOff(employeeDetails.getWeekOff());
        employee.setInvertPattern(employeeDetails.getInvertPattern());

        try {
            Employee updatedEmployee = employeeRepository.save(employee);
            return ResponseEntity.ok(updatedEmployee);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "An employee with that email already exists."));
        }
    }
    
    // DELETE - Delete employee
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Integer id) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    // Clear devices + login codes first so foreign keys don't
                    // block the delete and nothing is left orphaned.
                    employeeAccessService.revokeMobileAccess(employee.getId(), employee.getEmail());
                    employeeRepository.delete(employee);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}