package com.midco.rota.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.EmployeeRepository;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    
    private final EmployeeRepository employeeRepository;
    
    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
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
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // POST - Create new employee
    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        try {
            employee.setId(null);
            
            // preferredService now contains "ServiceName:Weight" format
            // No need to handle preferredLocations (removed)
            
            Employee savedEmployee = employeeRepository.save(employee);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // PUT - Update existing employee
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable Integer id, 
            @RequestBody Employee employeeDetails) {
        
        return employeeRepository.findById(id)
                .map(employee -> {
                    // Basic fields
                    employee.setFirstName(employeeDetails.getFirstName());
                    employee.setLastName(employeeDetails.getLastName());
                    employee.setGender(employeeDetails.getGender());
                    employee.setContractType(employeeDetails.getContractType());
                    employee.setMinHrs(employeeDetails.getMinHrs());
                    employee.setMaxHrs(employeeDetails.getMaxHrs());
                    employee.setRateCode(employeeDetails.getRateCode());
                    employee.setRestDays(employeeDetails.getRestDays());
                    
                    // Region and Services
                    employee.setPreferredRegion(employeeDetails.getPreferredRegion());
                    
                    // preferredService now stores "ServiceName:Weight" format
                    employee.setPreferredService(employeeDetails.getPreferredService());
                    employee.setRestrictedService(employeeDetails.getRestrictedService());
                    
                    // REMOVED: No more preferredLocations field
                    
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
                    
                    Employee updatedEmployee = employeeRepository.save(employee);
                    return ResponseEntity.ok(updatedEmployee);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // DELETE - Delete employee
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Integer id) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employeeRepository.delete(employee);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}