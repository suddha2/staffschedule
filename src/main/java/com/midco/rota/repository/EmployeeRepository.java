package com.midco.rota.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeSchedulePattern;
import com.midco.rota.util.ShiftType;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
	@Query(value = "SELECT * FROM employee WHERE preferred_region = :region", nativeQuery = true)
	List<Employee> findByPreferredRegion(@Param("region") String region);

	@Query("SELECT es FROM EmployeeSchedulePattern es " + "WHERE es.location = :location "
			+ "AND es.weekNumber = :week " + "AND es.dayOfWeek = :dayOfWeek " + "AND (es.shiftType = :sType) "
			+ "AND es.isAvailable = true " + "ORDER BY es.employee.id ASC")
	List<EmployeeSchedulePattern> findByPinnedEmp(@Param("location") String location, @Param("week") Integer week,
			@Param("dayOfWeek") String dayOfWeek, @Param("sType") ShiftType shiftType);

	@Query("SELECT e FROM Employee e WHERE e.firstName = :firstName AND e.lastName = :lastName")
	Employee findByFirstNameAndLastName(@Param("firstName") String firstName, @Param("lastName") String lastName);
}
