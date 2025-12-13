package com.midco.rota.service;

import com.midco.rota.model.Employee;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.Gender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Employee CRUD operations
 * Handles business logic, filtering, and pagination
 */
@Service
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Find employees with pagination, search, and filters
     */
    public Page<Employee> findEmployees(
            Pageable pageable,
            String search,
            String region,
            ContractType contractType,
            Gender gender
    ) {
        return employeeRepository.findAll(
            createSpecification(search, region, contractType, gender),
            pageable
        );
    }

    /**
     * Create dynamic specification for filtering
     */
    private Specification<Employee> createSpecification(
            String search,
            String region,
            ContractType contractType,
            Gender gender
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by name (first name OR last name)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate firstNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")),
                    searchPattern
                );
                Predicate lastNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")),
                    searchPattern
                );
                predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate));
            }

            // Filter by region
            if (region != null && !region.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("preferredRegion"), region));
            }

            // Filter by contract type
            if (contractType != null) {
                predicates.add(criteriaBuilder.equal(root.get("contractType"), contractType));
            }

            // Filter by gender
            if (gender != null) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Find employee by ID
     */
    public Optional<Employee> findById(Integer id) {
        return employeeRepository.findById(id);
    }

    /**
     * Check if employee exists by ID
     */
    public boolean existsById(Integer id) {
        return employeeRepository.existsById(id);
    }

    /**
     * Save (create or update) employee
     */
    public Employee save(Employee employee) {
        // Additional business logic can be added here
        // For example: validation, audit logging, etc.
        return employeeRepository.save(employee);
    }

    /**
     * Delete employee by ID
     */
    public void deleteById(Integer id) {
        employeeRepository.deleteById(id);
    }

    /**
     * Find all employees (without pagination)
     * Useful for dropdowns or exports
     */
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    /**
     * Count total employees
     */
    public long count() {
        return employeeRepository.count();
    }

    /**
     * Find employees by contract type
     */
    public List<Employee> findByContractType(ContractType contractType) {
        return employeeRepository.findByContractType(contractType);
    }

    /**
     * Find employees by preferred region
     */
    public List<Employee> findByPreferredRegion(String region) {
        return employeeRepository.findByPreferredRegion(region);
    }
}
