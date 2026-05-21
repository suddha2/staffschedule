package com.midco.rota.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.midco.rota.model.EmployeeDevice;

@Repository
public interface EmployeeDeviceRepository extends JpaRepository<EmployeeDevice, Long> {

	Optional<EmployeeDevice> findByFcmToken(String fcmToken);

	List<EmployeeDevice> findByEmployeeIdAndActiveTrue(Integer employeeId);

	List<EmployeeDevice> findByActiveTrue();

	/** Remove every device registration for an employee (on deactivation / delete). */
	long deleteByEmployeeId(Integer employeeId);
}
