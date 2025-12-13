package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeSchedulePattern;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.ShiftType;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer>, JpaSpecificationExecutor<Employee> {
	@Query(value = "SELECT * FROM employee WHERE preferred_region = :region", nativeQuery = true)
	List<Employee> findByPreferredRegion(@Param("region") String region);

	@Query("SELECT es FROM EmployeeSchedulePattern es " + "WHERE es.location = :location "
			+ "AND es.weekNumber = :week " + "AND es.dayOfWeek = :dayOfWeek " + "AND (es.shiftType = :sType) "
			+ "AND es.isAvailable = true " + "ORDER BY es.employee.id ASC")
	List<EmployeeSchedulePattern> findByPinnedEmp(@Param("location") String location, @Param("week") Integer week,
			@Param("dayOfWeek") String dayOfWeek, @Param("sType") ShiftType shiftType);

	@Query("SELECT e FROM Employee e WHERE e.firstName = :firstName AND e.lastName = :lastName")
	Employee findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);

	Page<Employee> findAll(Specification<Employee> specification, Pageable pageable);

	/**
	 * Find employees by contract type
	 */
	List<Employee> findByContractType(ContractType contractType);

	/**
	 * Find employees by gender
	 */
	@Query("SELECT e FROM Employee e WHERE e.gender = :gender")
	List<Employee> findByGender(@Param("gender") String gender);

	/**
	 * Search employees by name (first or last) Case-insensitive
	 */
	@Query("SELECT e FROM Employee e WHERE " + "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR "
			+ "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
	List<Employee> searchByName(@Param("name") String name);

	/**
	 * Find employees with specific skills Note: This requires custom implementation
	 * based on how skills are stored
	 */
//	@Query("SELECT e FROM Employee e WHERE :skill MEMBER OF e.skills")
//	List<Employee> findBySkill(@Param("skill") String skill);

	/**
	 * Count employees by contract type
	 */
	long countByContractType(ContractType contractType);

	/**
	 * Find employees by multiple contract types
	 */
	List<Employee> findByContractTypeIn(List<ContractType> contractTypes);

	/**
	 * Find all active employees (if you implement soft delete later) For now, this
	 * is just a template
	 */
	// @Query("SELECT e FROM Employee e WHERE e.active = true")
	// List<Employee> findAllActive();

}
